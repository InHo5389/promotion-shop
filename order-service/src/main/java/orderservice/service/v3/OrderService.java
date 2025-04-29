package orderservice.service.v3;

import event.EventType;
import event.payload.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.client.dto.CartProductResponse;
import orderservice.client.dto.CartResponse;
import orderservice.client.dto.ProductOptionRequest;
import orderservice.client.dto.ProductResponse;
import orderservice.client.serviceclient.CartServiceClient;
import orderservice.client.serviceclient.ProductServiceClient;
import orderservice.common.exception.CustomGlobalException;
import orderservice.common.exception.ErrorType;
import orderservice.entity.Order;
import orderservice.entity.OrderItem;
import orderservice.entity.OrderStatus;
import orderservice.service.OrderRepository;
import orderservice.service.component.CartValidator;
import orderservice.service.dto.*;
import orderservice.service.v2.DiscountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import outboxmessagerelay.OutboxEventPublisher;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Transactional Outbox 패턴 적용
 */
@Slf4j
@Service("orderServiceV3")
@RequiredArgsConstructor
public class OrderService {

    private final ProductServiceClient productClient;
    private final CartServiceClient cartClient;
    private final OrderRepository orderRepository;
    private final DiscountService discountService;
    private final CartValidator cartValidator;
    private final OutboxEventPublisher outboxEventPublisher;

    @Transactional
    public OrderResponse order(OrderRequest request) {
        log.info("Creating order for user {}", request.getUserId());

        // 1. 모든 상품 ID 수집
        List<Long> productIds = request.getItems().stream()
                .map(OrderItemRequest::getProductId)
                .toList();

        // 2. 상품 정보 일괄 조회
        log.info("start===============================");
        List<ProductResponse> products = productClient.getProducts(productIds);
        log.info("end===============================");
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

        order.setTotalAmount(totalAmount);
        discountService.calculateTotalDiscount(
                order,
                List.of(request.getCouponInfo()),
                request.getPoint()
        );

        Order savedOrder = orderRepository.save(order);

        // 4. 재고 감소 이벤트 발행 (비동기)
        StockDecreasedEventPayload stockPayload = StockDecreasedEventPayload.builder()
                .orderId(savedOrder.getId())
                .userId(request.getUserId())
                .items(stockUpdates.stream()
                        .map(update -> StockDecreasedEventPayload.StockItem.builder()
                                .productId(update.getProductId())
                                .productOptionId(update.getOptionId())
                                .quantity(update.getQuantity())
                                .build())
                        .collect(Collectors.toList()))
                .build();

        outboxEventPublisher.publish(EventType.STOCK_DECREASE, stockPayload);
        log.debug("Stock decrease event published for orderId={}", savedOrder.getId());

        // 4. 쿠폰 적용 이벤트 발행 (비동기)
        if (request.getCouponInfo() != null && request.getCouponInfo().getCouponId() != null) {
            CouponAppliedEventPayload couponPayload = CouponAppliedEventPayload.builder()
                    .orderId(savedOrder.getId())
                    .userId(request.getUserId())
                    .couponId(request.getCouponInfo().getCouponId())
                    .productId(request.getCouponInfo().getProductId())
                    .productOptionId(request.getCouponInfo().getProductOptionId())
                    .build();

            outboxEventPublisher.publish(EventType.COUPON_APPLIED, couponPayload);
            log.debug("Coupon applied event published for couponId={}", request.getCouponInfo().getCouponId());
        }

        // 포인트 사용 이벤트 발행 (비동기)
        if (request.getPoint() != null && request.getPoint() > 0) {
            PointUsedEventPayload pointPayload = PointUsedEventPayload.builder()
                    .orderId(savedOrder.getId())
                    .userId(request.getUserId())
                    .pointBalance(request.getPoint())
                    .build();

            outboxEventPublisher.publish(EventType.POINT_USED, pointPayload);
            log.debug("Point used event published for orderId={}, amount={}",
                    savedOrder.getId(), request.getPoint());
        }

        // 5. 주문 생성 이벤트 발행
        OrderCreatedEventPayload orderPayload = OrderCreatedEventPayload.builder()
                .orderId(savedOrder.getId())
                .userId(request.getUserId())
                .totalAmount(totalAmount)
                .discountAmount(savedOrder.getDiscountAmount())
                .pointAmount(savedOrder.getPointAmount())
                .finalAmount(savedOrder.getFinalAmount())
                .items(savedOrder.getOrderItems().stream()
                        .map(item -> OrderCreatedEventPayload.OrderItemData.builder()
                                .productId(item.getProductId())
                                .productOptionId(item.getProductOptionId())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .totalPrice(item.getTotalPrice())
                                .build())
                        .collect(Collectors.toList()))
                .couponInfo(request.getCouponInfo() != null ?
                        OrderCreatedEventPayload.CouponInfo.builder()
                                .couponId(request.getCouponInfo().getCouponId())
                                .productId(request.getCouponInfo().getProductId())
                                .productOptionId(request.getCouponInfo().getProductOptionId())
                                .build() : null)
                .build();

        outboxEventPublisher.publish(EventType.ORDER_CREATED, orderPayload);
        log.debug("Order created event published for orderId={}", savedOrder.getId());

        return OrderResponse.from(savedOrder);
    }

    @Transactional
    public OrderResponse cancel(Long orderId, Long userId) {
        log.info("Cancelling order orderId={}, userId={}", orderId, userId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.info("Order not found orderId={}", orderId);
                    return new CustomGlobalException(ErrorType.NOT_FOUND_ORDER);
                });

        if (!order.isOwnedBy(userId)) {
            log.info("Cancel permission denied orderId={}, orderUserId={}, requestUserId={}",
                    orderId, order.getUserId(), userId);
            throw new CustomGlobalException(ErrorType.NO_PERMISSION_TO_CANCEL_ORDER);
        }

        if (order.isCancelled()) {
            log.debug("Order already cancelled orderId={}", orderId);
            throw new CustomGlobalException(ErrorType.ORDER_ALREADY_CANCELED);
        }

        if (order.isShippedOrDeliveredOrRefunded()) {
            log.info("Order cannot be cancelled orderId={}, currentStatus={}",
                    orderId, order.getStatus());
            throw new CustomGlobalException(ErrorType.ORDER_CANNOT_BE_CANCELED);
        }

        order.setStatus(OrderStatus.CANCELLED);

        // 재고 증가 이벤트
        StockIncreasedEventPayload stockPayload = StockIncreasedEventPayload.builder()
                .orderId(orderId)
                .userId(userId)
                .items(order.getOrderItems().stream()
                        .map(item -> StockIncreasedEventPayload.StockItem.builder()
                                .productId(item.getProductId())
                                .productOptionId(item.getProductOptionId())
                                .quantity(item.getQuantity())
                                .build())
                        .collect(Collectors.toList()))
                .build();
        outboxEventPublisher.publish(EventType.STOCK_INCREASE, stockPayload);

        // 쿠폰 취소 이벤트

        CouponCanceledEventPayload couponPayload = CouponCanceledEventPayload.builder()
                .orderId(orderId)
                .userId(userId)
                .couponInfos(order.getOrderItems().stream()
                        .map(item -> CouponCanceledEventPayload.CouponInfo.builder()
                                .couponId(item.getCouponId())
                                .productId(item.getProductId())
                                .productOptionId(item.getProductOptionId())
                                .build())
                        .collect(Collectors.toList())
                )
                .build();
        outboxEventPublisher.publish(EventType.COUPON_CANCELED, couponPayload);

        // 포인트 취소 이벤트
        if (order.hasPointsUsed()) {
            PointEarnedEventPayload pointPayload = PointEarnedEventPayload.builder()
                    .orderId(orderId)
                    .userId(userId)
                    .pointBalance(order.getPointAmount().longValue())
                    .build();
            outboxEventPublisher.publish(EventType.POINT_CANCELED, pointPayload);
            log.debug("Point canceled event published for orderId={}, amount={}", orderId, order.getPointAmount());
        }

        log.debug("Order canceled event published for orderId={}", order.getId());
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse cartOrder(CartOrderRequest request) {
        log.info("Starting cart order for userId={}", request.getUserId());
        CartResponse cartResponse = cartClient.getCart(request.getUserId());
        log.debug("Cart retrieved for userId={}, items count={}", request.getUserId(), cartResponse.getItems().size());

        if (cartResponse.getItems().isEmpty()) {
            log.info("Empty cart order attempt for userId={}", request.getUserId());
            throw new CustomGlobalException(ErrorType.CART_EMPTY);
        }

        cartValidator.validateCartItems(cartResponse.getItems());

        Order order = createOrderFromCart(request, cartResponse.getItems());


        applyCoupons(order, request.getProductCoupons());

        // 주문 저장
        Order savedOrder = orderRepository.save(order);
        log.info("Order saved successfully orderId={}", savedOrder.getId());

        List<StockDecreasedEventPayload.StockItem> stockItems = cartResponse.getItems().stream()
                .map(item -> StockDecreasedEventPayload.StockItem.builder()
                        .productId(item.getProductId())
                        .productOptionId(item.getOptionId())
                        .quantity(item.getQuantity())
                        .build())
                .toList();

        // 재고 감소 이벤트 발행 (비동기)
        StockDecreasedEventPayload stockPayload = StockDecreasedEventPayload.builder()
                .orderId(savedOrder.getId())
                .userId(request.getUserId())
                .items(stockItems)
                .build();
        outboxEventPublisher.publish(EventType.STOCK_DECREASE, stockPayload);

        log.debug("Stock decrease event published for orderId={}", savedOrder.getId());

        // 쿠폰 적용 이벤트 발행 (비동기)
        for (ProductCouponInfo couponInfo : request.getProductCoupons()) {
            if (couponInfo.getCouponId() != null) {
                CouponAppliedEventPayload couponPayload = CouponAppliedEventPayload.builder()
                        .orderId(savedOrder.getId())
                        .userId(request.getUserId())
                        .couponId(couponInfo.getCouponId())
                        .productId(couponInfo.getProductId())
                        .productOptionId(couponInfo.getProductOptionId())
                        .build();

                outboxEventPublisher.publish(EventType.COUPON_APPLIED, couponPayload);
                log.debug("Coupon applied event published for couponId={}", couponInfo.getCouponId());
            }
        }

        // 장바구니 비우기 이벤트 발행 (비동기)
        CartClearedEventPayload cartPayload = CartClearedEventPayload.builder()
                .userId(request.getUserId())
                .orderId(savedOrder.getId())
                .build();

        outboxEventPublisher.publish(EventType.CART_CLEARED, cartPayload);
        log.debug("Cart cleared event published for userId={}", request.getUserId());

        // 주문 생성 이벤트 발행 (비동기)
        OrderCreatedEventPayload orderPayload = OrderCreatedEventPayload.builder()
                .orderId(savedOrder.getId())
                .userId(request.getUserId())
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .pointAmount(order.getPointAmount())
                .finalAmount(order.getFinalAmount())
                .items(savedOrder.getOrderItems().stream()
                        .map(item -> OrderCreatedEventPayload.OrderItemData.builder()
                                .productId(item.getProductId())
                                .productOptionId(item.getProductOptionId())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .totalPrice(item.getTotalPrice())
                                .build())
                        .collect(Collectors.toList()))
                .couponInfos(request.getProductCoupons().isEmpty() ? null : request.getProductCoupons().stream()
                        .map(couponInfo -> OrderCreatedEventPayload.CouponInfo.builder()
                                .couponId(couponInfo.getCouponId())
                                .productId(couponInfo.getProductId())
                                .productOptionId(couponInfo.getProductOptionId())
                                .build())
                        .collect(Collectors.toList()))
                .build();

        outboxEventPublisher.publish(EventType.ORDER_CREATED, orderPayload);
        log.debug("Order created event published for orderId={}", savedOrder.getId());

        return OrderResponse.from(savedOrder);
    }

    private Order createOrderFromCart(CartOrderRequest request, List<CartProductResponse> cartItems) {
        Order order = Order.builder()
                .userId(request.getUserId())
                .status(OrderStatus.PENDING)
                .address(request.getAddress())
                .receiverName(request.getReceiverName())
                .receiverPhone(request.getReceiverPhone())
                .paymentMethod(request.getPaymentMethod())
                .totalAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .pointAmount(BigDecimal.ZERO)
                .finalAmount(BigDecimal.ZERO)
                .paymentStatus("WAITING")
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartProductResponse cartItem : cartItems) {
            OrderItem orderItem = OrderItem.builder()
                    .productId(cartItem.getProductId())
                    .productOptionId(cartItem.getOptionId())
                    .productName(cartItem.getProductName())
                    .optionName(String.format("%s / %s", cartItem.getOptionSize(), cartItem.getOptionColor()))
                    .quantity(cartItem.getQuantity())
                    .unitPrice(cartItem.getPrice())
                    .totalPrice(cartItem.getTotalPrice())
                    .discountPrice(BigDecimal.ZERO)
                    .discountedTotalPrice(cartItem.getTotalPrice())
                    .build();

            order.addItem(orderItem);
            totalAmount = totalAmount.add(cartItem.getTotalPrice());
        }

        order.setTotalAmount(totalAmount);
        order.setFinalAmount(totalAmount);

        return order;
    }

    public void applyCoupons(Order order, List<ProductCouponInfo> productCoupons) {
        if (productCoupons == null || productCoupons.isEmpty()) {
            return;
        }

        // 상품/옵션 별 쿠폰 적용
        Map<String, OrderItem> orderItemMap = createOrderItemMap(order);
        List<ProductCouponInfo> validCoupons = filterValidCoupons(productCoupons, orderItemMap);

        // 주문 항목에 쿠폰 ID 설정
        assignCouponsToOrderItems(validCoupons, orderItemMap);

        // 할인 계산 서비스를 통해 할인 적용
        discountService.calculateDiscountOnly(order, validCoupons);

        // 할인 적용 후 금액 업데이트
        updateOrderAmounts(order);
    }

    private Map<String, OrderItem> createOrderItemMap(Order order) {
        return order.getOrderItems().stream()
                .collect(Collectors.toMap(
                        item -> item.getProductId() + ":" + item.getProductOptionId(),
                        item -> item
                ));
    }

    private List<ProductCouponInfo> filterValidCoupons(List<ProductCouponInfo> coupons, Map<String, OrderItem> orderItemMap) {
        List<ProductCouponInfo> validCoupons = coupons.stream()
                .filter(coupon -> orderItemMap.containsKey(coupon.getProductId() + ":" + coupon.getProductOptionId()))
                .toList();

        if (validCoupons.size() < coupons.size()) {
            log.warn("Some coupons were filtered out because the products are not in the cart. " +
                    "Original: {}, Valid: {}", coupons.size(), validCoupons.size());
        }

        return validCoupons;
    }

    private void assignCouponsToOrderItems(List<ProductCouponInfo> coupons, Map<String, OrderItem> orderItemMap) {
        for (ProductCouponInfo couponInfo : coupons) {
            String key = couponInfo.getProductId() + ":" + couponInfo.getProductOptionId();
            OrderItem item = orderItemMap.get(key);
            if (item != null) {
                item.setCouponId(couponInfo.getCouponId());
            }
        }
    }

    private void updateOrderAmounts(Order order) {
        // 각 주문 항목의 할인 금액 확인 및 보정
        for (OrderItem item : order.getOrderItems()) {
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
        }

        // 총 할인 금액 계산
        BigDecimal totalDiscountAmount = order.getOrderItems().stream()
                .map(OrderItem::getDiscountPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 주문의 할인 금액 설정
        order.setDiscountAmount(totalDiscountAmount);

        // 최종 결제 금액 계산 (총액 - 할인 - 포인트)
        BigDecimal finalAmount = order.getTotalAmount()
                .subtract(totalDiscountAmount)
                .subtract(order.getPointAmount() != null ? order.getPointAmount() : BigDecimal.ZERO);

        order.setFinalAmount(finalAmount);
    }
}