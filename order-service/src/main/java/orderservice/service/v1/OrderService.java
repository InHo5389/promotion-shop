package orderservice.service.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.client.serviceclient.CartServiceClient;
import orderservice.client.serviceclient.ProductServiceClient;
import orderservice.client.dto.*;
import orderservice.common.exception.CustomGlobalException;
import orderservice.common.exception.ErrorType;
import orderservice.entity.Order;
import orderservice.entity.OrderItem;
import orderservice.entity.OrderStatus;
import orderservice.service.DiscountService;
import orderservice.service.OrderRepository;
import orderservice.service.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final ProductServiceClient productClient;
    private final CartServiceClient cartClient;
    private final OrderRepository orderRepository;
    private final DiscountService discountService;

    @Transactional
    public OrderResponse order(OrderRequest request) {
        log.info("Creating order for user {}", request.getUserId());

        // 1. 모든 상품 ID 수집
        List<Long> productIds = request.getItems().stream()
                .map(OrderItemRequest::getProductId)
                .toList();

        // 2. 상품 정보 일괄 조회
        List<ProductResponse> products = productClient.getProducts(productIds);
        Map<Long, ProductResponse> productMap = products.stream()
                .collect(Collectors.toMap(
                        ProductResponse::getId,
                        p -> p));

        Order order = Order.create(request);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<ProductOptionRequest.StockUpdate> stockUpdates = new ArrayList<>();

        for (OrderItemRequest requestItem : request.getItems()) {
            ProductResponse product = productMap.get(requestItem.getProductId());

            OrderItem orderItem = OrderItem.create(requestItem, product);
            order.addItem(orderItem);
            totalAmount = totalAmount.add(orderItem.getTotalPrice());

            // 재고 업데이트 요청 수집
            stockUpdates.add(ProductOptionRequest.StockUpdate.create(
                    requestItem.getProductId(),
                    requestItem.getProductOptionId(),
                    requestItem.getQuantity()));
        }

        // 4. 재고 일괄 감소 (1회 API 호출)
        productClient.decreaseStock(stockUpdates);

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        // 쿠폰 적용 로직
        if (request.getCouponInfo() != null && request.getCouponInfo().getCouponId() != null) {
            try {
                // 주문 항목 중 쿠폰 적용 대상 찾기
                savedOrder.getOrderItems().stream()
                        .filter(item -> item.getProductId().equals(request.getCouponInfo().getProductId())
                                && item.getProductOptionId().equals(request.getCouponInfo().getProductOptionId()))
                        .findFirst()
                        .ifPresent(targetItem -> {
                            // 쿠폰 ID 설정
                            targetItem.setCouponId(request.getCouponInfo().getCouponId());

                            // 단일 쿠폰 적용
                            discountService.applyProductCoupons(savedOrder,
                                    Collections.singletonList(request.getCouponInfo()));

                            // 할인 적용 후 금액 업데이트
                            updateOrderAmountsAfterDiscount(savedOrder);

                            // 변경사항 저장
                            orderRepository.save(savedOrder);

                            log.info("Applied coupon ID: {} to order ID: {}",
                                    request.getCouponInfo().getCouponId(), savedOrder.getId());
                        });
            } catch (Exception e) {
                log.error("Error applying coupon: {}", e.getMessage());
                // 쿠폰 적용 실패 시에도 주문은 계속 처리
            }
        }

        return OrderResponse.from(savedOrder);
    }

    @Transactional
    public OrderResponse cancel(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_ORDER));

        if (!order.getUserId().equals(userId)) {
            throw new CustomGlobalException(ErrorType.NO_PERMISSION_TO_CANCEL_ORDER);
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new CustomGlobalException(ErrorType.ORDER_ALREADY_CANCELED);
        }

        if (order.getStatus() == OrderStatus.SHIPPED ||
                order.getStatus() == OrderStatus.DELIVERED ||
                order.getStatus() == OrderStatus.REFUNDED) {
            throw new CustomGlobalException(ErrorType.ORDER_CANNOT_BE_CANCELED);
        }

        order.setStatus(OrderStatus.CANCELLED);

        // 재고 일괄 증가 (1회 API 호출)
        List<ProductOptionRequest.StockUpdate> stockUpdates = order.getOrderItems().stream()
                .map(item -> ProductOptionRequest.StockUpdate.create(
                        item.getProductId(),
                        item.getProductOptionId(),
                        item.getQuantity()))
                .collect(Collectors.toList());

        if (!stockUpdates.isEmpty()) {
            productClient.increaseStock(stockUpdates);
        }

        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse cartOrder(CartOrderRequest request) {
        log.info("cartOrder");
        CartResponse cartResponse = cartClient.getCart(request.getUserId());
        log.info("cartClient.getCart");

        if (cartResponse.getItems().isEmpty()) {
            throw new CustomGlobalException(ErrorType.CART_EMPTY);
        }

        validateCartItems(cartResponse.getItems());

        Order order = Order.builder()
                .userId(request.getUserId())
                .status(OrderStatus.PENDING)
                .address(request.getAddress())
                .receiverName(request.getReceiverName())
                .receiverPhone(request.getReceiverPhone())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus("WAITING")
                .build();

        // 주문 항목 추가 및 재고 업데이트 요청 수집
        List<ProductOptionRequest.StockUpdate> stockUpdates = cartResponse.getItems().stream()
                .map(cartItem -> {
                    OrderItem orderItem = OrderItem.builder()
                            .productId(cartItem.getProductId())
                            .productOptionId(cartItem.getOptionId())
                            .productName(cartItem.getProductName())
                            .optionName(String.format("%s / %s", cartItem.getOptionSize(), cartItem.getOptionColor()))
                            .quantity(cartItem.getQuantity())
                            .unitPrice(cartItem.getPrice())
                            .totalPrice(cartItem.getTotalPrice())
                            .build();

                    order.addItem(orderItem);

                    return ProductOptionRequest.StockUpdate.create(
                            cartItem.getProductId(),
                            cartItem.getOptionId(),
                            cartItem.getQuantity());
                })
                .collect(Collectors.toList());

        // 쿠폰 적용 (제품별)
        if (request.getProductCoupons() != null && !request.getProductCoupons().isEmpty()) {
            // 주문 항목에 쿠폰 ID 설정
            request.getProductCoupons().forEach(couponInfo ->
                    order.getOrderItems().stream()
                            .filter(item -> item.getProductId().equals(couponInfo.getProductId())
                                    && item.getProductOptionId().equals(couponInfo.getProductOptionId()))
                            .findFirst()
                            .ifPresent(item -> item.setCouponId(couponInfo.getCouponId())));

            // 할인 적용
            discountService.applyProductCoupons(order, request.getProductCoupons());
        }

        // 할인 적용 후 금액 업데이트
        updateOrderAmountsAfterDiscount(order);

        // 주문 저장 및 재고 감소
        Order savedOrder = orderRepository.save(order);
        productClient.decreaseStock(stockUpdates);

        // 장바구니 비우기
        cartClient.clearCart(request.getUserId());

        return OrderResponse.from(savedOrder);
    }

    private void updateOrderAmountsAfterDiscount(Order order) {
        // 각 주문 항목의 할인 관련 필드 초기화
        order.getOrderItems().forEach(item -> {
            if (item.getCouponId() != null) {
                // 쿠폰이 적용된 항목이지만 할인 금액이 설정되지 않은 경우
                if (item.getDiscountPrice() == null) {
                    item.setDiscountPrice(BigDecimal.ZERO);
                }
                // 쿠폰이 적용된 항목이지만 할인 적용 후 금액이 설정되지 않은 경우
                if (item.getDiscountedTotalPrice() == null) {
                    item.setDiscountedTotalPrice(item.getTotalPrice().subtract(item.getDiscountPrice()));
                }
            } else {
                // 쿠폰이 적용되지 않은 항목
                item.setDiscountPrice(BigDecimal.ZERO);
                item.setDiscountedTotalPrice(item.getTotalPrice());
            }
        });

        // 주문 총액 재계산 (할인 적용 후)
        BigDecimal finalTotalAmount = order.getOrderItems().stream()
                .map(OrderItem::getDiscountedTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(finalTotalAmount);
    }

    private void validateCartItems(List<CartProductResponse> cartItems) {
        // 상품 ID 수집
        List<Long> productIds = cartItems.stream()
                .map(CartProductResponse::getProductId)
                .distinct()
                .toList();

        // 상품 정보 일괄 조회
        List<ProductResponse> products = productClient.getProducts(productIds);

        Map<Long, ProductResponse> productMap = products.stream()
                .collect(Collectors.toMap(
                        ProductResponse::getId,
                        p -> p,
                        (existing, replacement) -> existing));

        // 각 장바구니 아이템에 대해 재고 검증
        for (CartProductResponse cartItem : cartItems) {
            ProductResponse product = productMap.get(cartItem.getProductId());

            // 상품이 판매 중인지 확인
            if (!"ACTIVE".equals(product.getStatus())) {
                throw new CustomGlobalException(ErrorType.PRODUCT_NOT_SELL);
            }

            // 옵션을 찾아 재고 확인
            ProductResponse.ProductOptionDTO option = product.getOptions().stream()
                    .filter(opt -> opt.getId().equals(cartItem.getOptionId()))
                    .findFirst()
                    .orElseThrow(() -> new CustomGlobalException(ErrorType.OPTION_NOT_FOUND));

            if (option.getStockQuantity() < cartItem.getQuantity()) {
                throw new CustomGlobalException(ErrorType.NOT_ENOUGH_STOCK);
            }
        }
    }
}