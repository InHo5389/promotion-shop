package orderservice.service.v1;

import event.EventType;
import event.payload.OrderConfirmPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.client.dto.*;
import orderservice.client.serviceclient.CouponServiceClient;
import orderservice.client.serviceclient.PointServiceClient;
import orderservice.client.serviceclient.ProductServiceClient;
import orderservice.common.exception.CustomGlobalException;
import orderservice.common.exception.ErrorType;
import orderservice.entity.*;
import orderservice.repository.CompensationRegistryJpaRepository;
import orderservice.repository.OrderJpaRepository;
import orderservice.service.dto.request.OrderItemInfo;
import orderservice.service.dto.request.OrderRequest;
import orderservice.service.dto.response.OrderResponse;
import orderservice.service.kafka.producer.OrderEventProducer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import outboxmessagerelay.OutboxEventPublisher;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderJpaRepository orderRepository;
    private final ProductServiceClient productClient;
    private final CouponServiceClient couponClient;
    private final PointServiceClient pointClient;
    private final CartService cartService;
    private final OrderEventProducer orderEventProducer;
    private final OutboxEventPublisher outboxEventPublisher;
    private final CompensationRegistryJpaRepository compensationRegistryJpaRepository;

    @Transactional
    public OrderResponse.Create createOrderFromCart(Long userId, OrderRequest.Create request) {
        log.info("===== 장바구니 전체 주문 생성 시작 ===== userId: {}", userId);

        // 1. Redis에서 장바구니 전체 조회
        List<CartItemRedis> cartItems = cartService.getCartItems(userId);
        log.info("장바구니 조회 완료 - userId: {}, itemCount: {}", userId, cartItems.size());
        if (cartItems.isEmpty()) {
            throw new CustomGlobalException(ErrorType.EMPTY_CART);
        }

        // 2. 상품 정보 조회 및 금액 계산
        List<OrderItemInfo> orderItemInfos = calculateOrderItemsFromCart(cartItems);
        int totalAmount = orderItemInfos.stream()
                .mapToInt(OrderItemInfo::getTotalPrice)
                .sum();
        log.info("주문 금액 계산 완료 - totalAmount: {}", totalAmount);

        // 3. 주문 엔티티 생성 (PENDING)
        Order order = createPendingOrder(userId, totalAmount, orderItemInfos, request);
        log.info("주문 엔티티 생성 완료 - orderId: {}, status: PENDING", order.getId());

        int couponDiscount = 0;
        int pointDiscount = 0;

        try {
            // 4. 재고 예약 (동기 Feign)
            reserveStock(order);

            // 5. 쿠폰 예약 (동기 Feign)
            couponDiscount = reserveCoupons(order);

            // 6. 포인트 예약 (동기 Feign)
            pointDiscount = reservePoint(userId, order, request.getUsePoint());

            // 7. 최종 금액 계산 및 할인 적용
            int finalAmount = totalAmount - couponDiscount - pointDiscount;
            order.applyDiscounts(couponDiscount, pointDiscount, finalAmount);
            orderRepository.save(order);
            log.info("할인 적용 완료 - couponDiscount: {}, pointDiscount: {}, finalAmount: {}",
                    couponDiscount, pointDiscount, finalAmount);

            // 8. 장바구니 비우기
            cartService.clearCart(userId);

            log.info("===== 장바구니 주문 생성 완료 ===== orderId: {}, finalAmount: {}",
                    order.getId(), finalAmount);

            return OrderResponse.Create.from(order);

        } catch (Exception e) {
            rollbackOrderCreation(order, couponDiscount, pointDiscount);
            throw e;
        }
    }

    public void rollbackOrderCreation(Order order, int couponDiscount, int pointDiscount) {
        try {
            log.error("주문 생성 실패 - orderId: {}", order.getId());

            productClient.cancelReservation(order.getId());

            if (couponDiscount > 0) {
                couponClient.cancelReservation(order.getId());
            }

            if (pointDiscount > 0) {
                pointClient.cancelReservation(order.getId());
            }

            order.fail();
            orderRepository.save(order);
        } catch (Exception e) {
            compensationRegistryJpaRepository.save(new CompensationRegistry(
                    order.getId(), CompensationRegistry.CompensationType.ORDER_CREATE_ROLLBACK));
            throw e;
        }
    }

    @Transactional
    public OrderResponse.Confirm confirmOrder(Long orderId) {
        log.info("===== 주문 확정 처리 시작 ===== orderId: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_ORDER));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new CustomGlobalException(ErrorType.INVALID_ORDER_STATUS);
        }

        order.confirm();
        log.info("주문 상태 변경 완료 - orderId: {}, status: CONFIRMED", orderId);

        OrderConfirmPayload event = OrderConfirmPayload.builder()
                .orderId(orderId)
                .userId(order.getUserId())
                .build();

        outboxEventPublisher.publish(EventType.ORDER_CONFIRM, event);

        log.info("===== 주문 확정 처리 완료 ===== orderId: {}", orderId);
        return OrderResponse.Confirm.from(order);
    }

    @Transactional
    public OrderResponse.Cancel cancelOrder(Long orderId, Long userId) {
        log.info("===== 주문 취소 처리 시작 ===== orderId: {}, userId: {}", orderId, userId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_ORDER));

//        validateOrderExpiration(order);

        if (order.getStatus() != OrderStatus.PENDING &&
                order.getStatus() != OrderStatus.CONFIRMED) {
            throw new CustomGlobalException(ErrorType.INVALID_ORDER_STATUS);
        }

        OrderStatus currentStatus = order.getStatus();
        log.info("현재 주문 상태: {} - orderId: {}", currentStatus, orderId);

        if (currentStatus == OrderStatus.PENDING) {
            cancelPendingOrder(order);
        } else {
            cancelConfirmedOrder(order);
        }

        order.cancel();

        log.info("===== 주문 취소 처리 완료 ===== orderId: {}", orderId);
        return OrderResponse.Cancel.from(order);
    }

    /**
     * PENDING 상태 취소 (결제 전)
     * - 예약만 해제
     */
    private void cancelPendingOrder(Order order) {
        Long orderId = order.getId();

        try {
            // 재고 예약 해제
            productClient.cancelReservation(orderId);

            // 쿠폰 예약 해제
            if (order.getCouponDiscount() > 0) {
                couponClient.cancelReservation(orderId);
            }

            // 포인트 예약 해제
            if (order.getPointDiscount() > 0) {
                pointClient.cancelReservation(orderId);
            }

        } catch (Exception e) {
            rollbackOrderPendingCancel(order);
            throw e;
        }
    }

    public void rollbackOrderPendingCancel(Order order) {
        try {
            log.error("PENDING 주문 취소 실패 - orderId: {}", order.getId());

            productClient.rollbackReserveStock(order.getId());

            if (order.getCouponDiscount() > 0) {
                couponClient.rollbackReserveCoupon(order.getId());
            }

            if (order.getPointDiscount() > 0) {
                pointClient.rollbackReservePoints(order.getId());
            }

            order.fail();
            orderRepository.save(order);

            throw new CustomGlobalException(ErrorType.ORDER_CANCEL_FAILED);
        } catch (Exception e) {
            compensationRegistryJpaRepository.save(new CompensationRegistry(
                    order.getId(), CompensationRegistry.CompensationType.ORDER_CANCEL_PENDING));
            throw e;
        }
    }

    /**
     * CONFIRMED 상태 취소 (결제 후)
     * - 확정 차감 복구
     */
    private void cancelConfirmedOrder(Order order) {
        Long orderId = order.getId();

        try {
            productClient.rollbackConfirmStock(orderId);

            if (order.getCouponDiscount() > 0) {
                couponClient.rollbackConfirmCoupon(orderId);
            }

            if (order.getPointDiscount() > 0) {
                pointClient.rollbackConfirmPoints(orderId);
            }

            // TODO: PG사 환불
            // paymentClient.refund(orderId);

        } catch (Exception e) {
            rollbackOrderConfirmCancel(order);
            throw e;
        }
    }

    public void rollbackOrderConfirmCancel(Order order) {
        try {
            log.error("CONFIRMED 주문 취소 실패 - orderId: {}", order.getId());

            productClient.confirmStock(order.getId());

            if (order.getCouponDiscount() > 0) {
                couponClient.confirmCoupon(order.getId());
            }

            if (order.getPointDiscount() > 0) {
                pointClient.confirmPoints(order.getId());
            }

            order.fail();
            orderRepository.save(order);

            throw new CustomGlobalException(ErrorType.ORDER_CANCEL_FAILED);

        } catch (Exception e) {
            compensationRegistryJpaRepository.save(new CompensationRegistry(
                    order.getId(), CompensationRegistry.CompensationType.ORDER_CANCEL_CONFIRMED));
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse.Detail getOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_ORDER));

        validateOrderOwnership(order, userId);

        return OrderResponse.Detail.from(order);
    }

    @Transactional
    public void cancelExpiredOrder(Long orderId) {
        log.info("===== 만료 주문 자동 취소 시작 ===== orderId: {}", orderId);

        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_ORDER));

            if (order.getStatus() != OrderStatus.PENDING) {
                log.warn("이미 처리된 주문 - orderId: {}, status: {}", orderId, order.getStatus());
                return;
            }

            order.cancel();
            orderRepository.save(order);

            orderEventProducer.sendStockCancelEvent(orderId);
            orderEventProducer.sendCouponCancelEvent(orderId);
            orderEventProducer.sendPointCancelEvent(orderId, order.getUserId());

            log.info("===== 만료 주문 자동 취소 완료 ===== orderId: {}", orderId);

        } catch (Exception e) {
            log.error("만료 주문 취소 실패 - orderId: {}", orderId, e);
            throw e;
        }
    }

    private List<OrderItemInfo> calculateOrderItemsFromCart(List<CartItemRedis> cartItems) {
        List<OrderItemInfo> orderItemInfos = new ArrayList<>();

        for (CartItemRedis cartItem : cartItems) {
            ProductResponse product = productClient.read(cartItem.getProductId());
            ProductOptionDto option = productClient.getProductOption(cartItem.getProductOptionId());

            int itemPrice = product.getPrice().intValue() + option.getAdditionalPrice().intValue();
            int itemTotalPrice = itemPrice * cartItem.getQuantity();

            Integer couponDiscount = cartItem.getCouponDiscount() != null ? cartItem.getCouponDiscount() : 0;

            OrderItemInfo itemInfo = OrderItemInfo.builder()
                    .productId(cartItem.getProductId())
                    .productOptionId(cartItem.getProductOptionId())
                    .quantity(cartItem.getQuantity())
                    .price(itemPrice)
                    .totalPrice(itemTotalPrice)
                    .couponId(cartItem.getAppliedCouponId())
                    .couponDiscount(couponDiscount)
                    .build();

            orderItemInfos.add(itemInfo);
        }

        return orderItemInfos;
    }

    private Order createPendingOrder(
            Long userId,
            int totalAmount,
            List<OrderItemInfo> orderItemInfos,
            OrderRequest.Create request
    ) {
        Order order = Order.create(userId, totalAmount, 0, 0, totalAmount);

        order.setAddress(request.getAddress());
        order.setReceiverName(request.getReceiverName());
        order.setReceiverPhone(request.getReceiverPhone());
        order.setPaymentMethod(request.getPaymentMethod());

        for (OrderItemInfo itemInfo : orderItemInfos) {
            OrderItem orderItem = OrderItem.create(
                    order,
                    itemInfo.getProductId(),
                    itemInfo.getProductOptionId(),
                    itemInfo.getQuantity(),
                    itemInfo.getPrice(),
                    itemInfo.getCouponId(),
                    itemInfo.getCouponDiscount()
            );
            order.addItem(orderItem);
        }

        return orderRepository.save(order);
    }

    private void reserveStock(Order order) {

        List<StockReserveRequest.OrderItem> items = order.getOrderItems().stream()
                .map(item -> StockReserveRequest.OrderItem.builder()
                        .productOptionId(item.getProductOptionId())
                        .quantity(item.getQuantity())
                        .build())
                .toList();

        StockReserveRequest request = StockReserveRequest.builder()
                .orderId(order.getId())
                .items(items)
                .build();

        productClient.reserveStock(request);
        log.info("재고 예약 성공 - orderId: {}", order.getId());
    }

    private int reserveCoupons(Order order) {

        List<CouponReserveRequest.CouponItem> couponItems = order.getOrderItems().stream()
                .filter(item -> item.getCouponId() != null)
                .map(item -> CouponReserveRequest.CouponItem.builder()
                        .couponId(item.getCouponId())
                        .productOptionId(item.getProductOptionId())
                        .productPrice(item.getTotalPrice())
                        .build())
                .toList();

        if (couponItems.isEmpty()) {
            log.info("쿠폰 사용 없음 - orderId: {}", order.getId());
            return 0;
        }

        CouponReserveRequest request = CouponReserveRequest.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .couponItems(couponItems)
                .build();

        CouponReserveResponse response = couponClient.reserveCoupon(request);
        log.info("쿠폰 예약 성공 - orderId: {}, discount: {}", order.getId(), response.totalDiscount());

        return response.totalDiscount();
    }

    private int reservePoint(Long userId, Order order, Integer usePoint) {
        int pointDiscount = usePoint != null ? usePoint : 0;

        if (pointDiscount <= 0) {
            log.info("포인트 사용 없음 - orderId: {}", order.getId());
            return 0;
        }

        PointReserveRequest request = PointReserveRequest.builder()
                .orderId(order.getId())
                .userId(userId)
                .amount((long) pointDiscount)
                .build();

        pointClient.reservePoints(request);
        log.info("포인트 예약 성공 - orderId: {}, point: {}", order.getId(), pointDiscount);

        return pointDiscount;
    }

    private void validateOrderExpiration(Order order) {
        if (order.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("주문 만료 - orderId: {}", order.getId());
            order.cancel();
            orderRepository.save(order);

            orderEventProducer.sendStockCancelEvent(order.getId());
            orderEventProducer.sendCouponCancelEvent(order.getId());
            orderEventProducer.sendPointCancelEvent(order.getId(), order.getUserId());

            throw new CustomGlobalException(ErrorType.ORDER_EXPIRED);
        }
    }

    private void validateOrderOwnership(Order order, Long userId) {
        if (!order.getUserId().equals(userId)) {
            throw new CustomGlobalException(ErrorType.UNAUTHORIZED_ORDER_ACCESS);
        }
    }

    private Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }
}