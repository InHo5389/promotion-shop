package productservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import productservice.common.exception.CustomGlobalException;
import productservice.common.exception.ErrorType;
import productservice.entity.*;
import productservice.repository.*;
import productservice.service.dto.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static productservice.entity.ProductTransactionHistory.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductStockJpaRepository productStockJpaRepository;

    private final CategoryJpaRepository categoryJpaRepository;
    private final ProductJpaRepository productJpaRepository;
    private final ProductOptionJpaRepository productOptionJpaRepository;
    private final ProductTransactionJpaRepository productTransactionJpaRepository;
    private final ProductDocumentRepository productDocumentRepository;

    @Transactional
    public ProductResponse create(ProductRequest.Create request) {
        log.info("상품 생성 요청 - 카테고리ID: {}, 상품명: {}, 가격: {}",
                request.getCategoryId(), request.getName(), request.getPrice());

        Category category = categoryJpaRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_CATEGORY));
        Product product = Product.create(category, request.getName(), request.getPrice(), request.getImage());

        request.getOptions().forEach(optionReq -> {
            ProductOption option = ProductOption.create(
                    product,
                    optionReq.getSize(),
                    optionReq.getColor(),
                    optionReq.getAdditionalPrice()
            );

            ProductStock stock = ProductStock.create(
                    option,
                    optionReq.getStockQuantity()
            );

            option.setStock(stock);
            product.addOption(option);
        });

        Product savedProduct = productJpaRepository.save(product);
        log.info("상품 생성 완료 - 상품ID: {}, 상품명: {}",
                savedProduct.getId(), savedProduct.getName());

        ProductDocument productDocument = productDocumentRepository.save(ProductDocument.from(savedProduct));
        log.info("ES 상품 저장 완료 - 상품ID: {}, 상품명: {}", productDocument.getId(), productDocument.getName());

        return ProductResponse.from(savedProduct);
    }

    @Transactional
    public void reserveStock(StockReserveRequest request) {
        Long orderId = request.orderId();

        log.info("===== 재고 예약 시작 ===== orderId: {}", orderId);

        // 중복 체크: 이미 처리된 주문인지 확인
        List<ProductTransactionHistory> existingHistories =
                productTransactionJpaRepository.findByOrderIdAndType(orderId, TransactionType.RESERVE);

        if (!existingHistories.isEmpty()) {
            log.warn("이미 재고가 예약된 주문 - orderId: {}", orderId);
            return;
        }

        for (StockReserveRequest.OrderItem item : request.items()) {
            ProductStock stock = productStockJpaRepository.findByProductOptionId(item.productOptionId())
                    .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_STOCK));

            stock.reserve(item.quantity());

            ProductTransactionHistory history = ProductTransactionHistory.create(
                    orderId,
                    item.productOptionId(),
                    item.quantity(),
                    TransactionType.RESERVE
            );
            productTransactionJpaRepository.save(history);

            log.info("재고 예약 완료 - orderId: {}, productOptionId: {}, quantity: {}",
                    orderId, item.productOptionId(), item.quantity());
        }
    }

    @Transactional
    public void confirmReservation(Long orderId) {
        log.info("===== 재고 확정 시작 ===== orderId: {}", orderId);

        // 멱등성 체크: 이미 확정된 주문인지 확인
        List<ProductTransactionHistory> confirmHistories =
                productTransactionJpaRepository.findByOrderIdAndType(orderId, TransactionType.CONFIRM_RESERVE);

        if (!confirmHistories.isEmpty()) {
            log.warn("이미 재고가 확정된 주문 - orderId: {}", orderId);
            return;
        }

        List<ProductTransactionHistory> reserveHistories =
                productTransactionJpaRepository.findByOrderIdAndType(orderId, TransactionType.RESERVE);

        if (reserveHistories.isEmpty()) {
            log.info("재고 확정 스킵 - 예약 내역 없음, orderId: {}", orderId);
            throw new CustomGlobalException(ErrorType.NOT_FOUND_RESERVE_STOCK);
//            return;
        }

        for (ProductTransactionHistory history : reserveHistories) {
            ProductStock stock = productStockJpaRepository.findByProductOptionId(history.getProductOptionId())
                    .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_STOCK));

            // 예약 확정 (실제 차감)
            stock.confirmReservation(history.getQuantity());

            // 확정 이력 저장
            ProductTransactionHistory confirmHistory = ProductTransactionHistory.create(
                    orderId,
                    history.getProductOptionId(),
                    history.getQuantity(),
                    TransactionType.CONFIRM_RESERVE
            );
            productTransactionJpaRepository.save(confirmHistory);

            log.info("재고 확정 완료 - orderId: {}, productOptionId: {}",
                    orderId, history.getProductOptionId());
        }
    }

    @Transactional
    public void cancelReservation(Long orderId) {
        log.info("===== 재고 예약 취소 시작 ===== orderId: {}", orderId);

        // 멱등성 체크: 이미 취소된 주문인지 확인
        List<ProductTransactionHistory> cancelHistories =
                productTransactionJpaRepository.findByOrderIdAndType(orderId, TransactionType.CANCEL_RESERVE);

        if (!cancelHistories.isEmpty()) {
            log.info("재고 취소 스킵 - 이미 처리됨, orderId: {}", orderId);
            return; // 예약이 없으면 취소할 것도 없음
        }

        List<ProductTransactionHistory> reserveHistories =
                productTransactionJpaRepository.findByOrderIdAndType(orderId, TransactionType.RESERVE);

        if (reserveHistories.isEmpty()) {
            log.info("재고 취소 스킵 - 예약 내역 없음, orderId: {}", orderId);
            return;
        }

        for (ProductTransactionHistory reserveHistory : reserveHistories) {
            ProductStock stock = productStockJpaRepository.findByProductOptionId(reserveHistory.getProductOptionId())
                    .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_STOCK));

            // 예약 취소
            stock.cancelReservation(reserveHistory.getQuantity());

            // 취소 이력 저장
            ProductTransactionHistory cancelHistory = ProductTransactionHistory.create(
                    orderId,
                    reserveHistory.getProductOptionId(),
                    reserveHistory.getQuantity(),
                    TransactionType.CANCEL_RESERVE
            );
            productTransactionJpaRepository.save(cancelHistory);

            log.info("재고 취소 완료 - orderId: {}, productOptionId: {}",
                    orderId, reserveHistory.getProductOptionId());
        }
    }

    @Transactional
    public void rollbackConfirmation(Long orderId) {
        log.info("===== 재고 확정 롤백 시작 ===== orderId: {}", orderId);

        // 멱등성 체크: 이미 롤백된 주문인지 확인
        List<ProductTransactionHistory> rollbackHistories =
                productTransactionJpaRepository.findByOrderIdAndType(orderId, TransactionType.ROLLBACK_CONFIRM);

        if (!rollbackHistories.isEmpty()) {
            log.warn("이미 재고 확정이 롤백된 주문 - orderId: {}", orderId);
            return;
        }

        List<ProductTransactionHistory> confirmHistories =
                productTransactionJpaRepository.findByOrderIdAndType(orderId, TransactionType.CONFIRM_RESERVE);

        if (confirmHistories.isEmpty()) {
            log.warn("재고 확정 히스토리 없음 - orderId: {}", orderId);
            throw new CustomGlobalException(ErrorType.STOCK_CONFIRMATION_NOT_FOUND);
        }

        for (ProductTransactionHistory history : confirmHistories) {
            ProductStock stock = productStockJpaRepository.findByProductOptionId(history.getProductOptionId())
                    .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_STOCK));

            // Confirm 상태를 Reserve로 되돌림
            stock.rollbackConfirmation(history.getQuantity());

            ProductTransactionHistory cancelHistory = ProductTransactionHistory.create(
                    orderId,
                    history.getProductOptionId(),
                    history.getQuantity(),
                    TransactionType.ROLLBACK_CONFIRM
            );
            productTransactionJpaRepository.save(cancelHistory);

            log.info("재고 확정 롤백 완료 - orderId: {}, optionId: {}, quantity: {}",
                    orderId, history.getProductOptionId(), history.getQuantity());
        }

        log.info("재고 확정 롤백 완료. orderId: {}", orderId);
    }

    @Transactional
    public void rollbackReservation(Long orderId) {
        log.info("===== 재고 예약 롤백 시작 ===== orderId: {}", orderId);

        // 멱등성 체크: 이미 롤백된 주문인지 확인
        List<ProductTransactionHistory> rollbackHistories =
                productTransactionJpaRepository.findByOrderIdAndType(orderId, TransactionType.ROLLBACK_RESERVE);

        if (!rollbackHistories.isEmpty()) {
            log.warn("이미 재고 확정이 롤백된 주문 - orderId: {}", orderId);
            return;
        }

        List<ProductTransactionHistory> cancelReserveHistories =
                productTransactionJpaRepository.findByOrderIdAndType(orderId, TransactionType.CANCEL_RESERVE);

        if (cancelReserveHistories.isEmpty()) {
            log.warn("재고 예약 취소 히스토리 없음 - orderId: {}", orderId);
            throw new CustomGlobalException(ErrorType.STOCK_CONFIRMATION_NOT_FOUND);
        }

        for (ProductTransactionHistory history : cancelReserveHistories) {
            ProductStock stock = productStockJpaRepository.findByProductOptionId(history.getProductOptionId())
                    .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_STOCK));

            // Confirm 상태를 Reserve로 되돌림
            stock.rollbackReservation(history.getQuantity());

            ProductTransactionHistory cancelHistory = ProductTransactionHistory.create(
                    orderId,
                    history.getProductOptionId(),
                    history.getQuantity(),
                    TransactionType.ROLLBACK_RESERVE
            );
            productTransactionJpaRepository.save(cancelHistory);

            log.info("재고 확정 롤백 완료 - orderId: {}, optionId: {}, quantity: {}",
                    orderId, history.getProductOptionId(), history.getQuantity());
        }

        log.info("재고 확정 롤백 완료. orderId: {}", orderId);
    }

    @Transactional
    public ProductResponse update(Long productId, ProductRequest.Update request) {
        log.info("상품 수정 요청 - 상품ID: {}", productId);

        Product product = productJpaRepository.findById(productId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_PRODUCT));

        if (request.getCategoryId() != null) {
            Category category = categoryJpaRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_CATEGORY));
            product.updateCategory(category);
        }

        product.update(request.getName(), request.getPrice(), request.getImage(), request.getStatus());

        if (request.getOptions() != null) {
            product.updateOptions(request.getOptions());
        }

        log.info("상품 수정 완료 - 상품ID: {}", productId);
        return ProductResponse.from(productJpaRepository.save(product));
    }

    public ProductResponse read(Long productId) {
        Product product = productJpaRepository.findByIdWithFetchJoin(productId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_PRODUCT));

        return ProductResponse.from(product);
    }

    public void delete(Long productId) {
        Product product = productJpaRepository.findByIdWithFetchJoin(productId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_PRODUCT));

        productJpaRepository.delete(product);

        productDocumentRepository.deleteById(product.getId().toString());
    }

    public void increaseStock(List<ProductOptionRequest.StockUpdate> requests) {
        if (requests.isEmpty()) return;

        log.info("재고 증가 요청 - 옵션 수: {}", requests.size());

        List<Long> optionIds = requests.stream()
                .map(ProductOptionRequest.StockUpdate::getOptionId)
                .toList();

        List<ProductOption> options = productOptionJpaRepository.findAllWithStockByIdIn(optionIds);
        Map<Long, ProductOption> optionMap = options.stream()
                .collect(Collectors.toMap(ProductOption::getId, option -> option));

        for (ProductOptionRequest.StockUpdate request : requests) {
            ProductOption option = optionMap.get(request.getOptionId());

            if (option == null) {
                throw new CustomGlobalException(ErrorType.NOT_FOUND_PRODUCT_OPTION);
            }

            option.getStock().updateQuantity(option.getStock().getQuantity() + request.getQuantity());
        }

        productOptionJpaRepository.saveAll(options);

        log.info("재고 증가 처리 완료 - 처리된 옵션 수: {}", requests.size());
    }

    public List<ProductResponse> getProductByIds(List<Long> productIds) {
        List<Product> products = productJpaRepository.findAllWithCategoryOptionsAndStockByIdIn(productIds);

        // 요청한 ID 순서대로 결과 정렬
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        return productIds.stream()
                .filter(productMap::containsKey)
                .map(productMap::get)
                .map(ProductResponse::from)
                .collect(Collectors.toList());
    }

    public ProductOptionDto getProductOption(Long productOptionId){
        ProductOption productOption = productOptionJpaRepository.findById(productOptionId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_PRODUCT_OPTION));
        return ProductOptionDto.from(productOption);
    }

}
