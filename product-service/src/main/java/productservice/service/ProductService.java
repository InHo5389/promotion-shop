package productservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import productservice.common.exception.CustomGlobalException;
import productservice.common.exception.ErrorType;
import productservice.entity.Category;
import productservice.entity.Product;
import productservice.entity.ProductOption;
import productservice.entity.ProductStock;
import productservice.repository.CategoryJpaRepository;
import productservice.repository.ProductJpaRepository;
import productservice.repository.ProductOptionJpaRepository;
import productservice.repository.ProductStockJpaRepository;
import productservice.service.dto.ProductOptionRequest;
import productservice.service.dto.ProductRequest;
import productservice.service.dto.ProductResponse;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final CategoryJpaRepository categoryJpaRepository;
    private final ProductJpaRepository productJpaRepository;
    private final ProductOptionJpaRepository productOptionJpaRepository;

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
        return ProductResponse.from(savedProduct);
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
    }

    @Transactional
    public void decreaseStock(List<ProductOptionRequest.StockUpdate> requests) {
        if (requests.isEmpty()) return;

        log.info("재고 감소 요청 - 옵션 수: {}", requests.size());

        List<Long> optionIds = requests.stream()
                .map(ProductOptionRequest.StockUpdate::getOptionId)
                .toList();

        List<ProductOption> options = productOptionJpaRepository.findAllByIdIn(optionIds);
        Map<Long, ProductOption> optionMap = options.stream()
                .collect(Collectors.toMap(ProductOption::getId, option -> option));

        for (ProductOptionRequest.StockUpdate request : requests) {
            ProductOption option = optionMap.get(request.getOptionId());

            if (option == null) {
                throw new CustomGlobalException(ErrorType.NOT_FOUND_PRODUCT_OPTION);
            }

            if (option.getStock().getQuantity() < request.getQuantity()) {
                log.warn("재고 감소 실패 - 재고 부족 - 옵션ID: {}, 요청 수량: {}, 현재 재고: {}",
                        request.getOptionId(), request.getQuantity(),option.getSize());
                throw new CustomGlobalException(ErrorType.NOT_ENOUGH_STOCK);
            }

            option.getStock().updateQuantity(option.getStock().getQuantity() - request.getQuantity());
        }

        productOptionJpaRepository.saveAll(options);

        log.info("재고 감소 처리 완료 - 처리된 옵션 수: {}", requests.size());
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
}
