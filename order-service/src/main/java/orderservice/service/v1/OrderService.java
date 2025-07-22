package orderservice.service.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.client.dto.*;
import orderservice.client.serviceclient.CartServiceClient;
import orderservice.client.serviceclient.CouponServiceClient;
import orderservice.client.serviceclient.PointServiceClient;
import orderservice.client.serviceclient.ProductServiceClient;
import orderservice.common.exception.CompensationException;
import orderservice.common.exception.CustomGlobalException;
import orderservice.common.exception.ErrorType;
import orderservice.entity.Order;
import orderservice.entity.OrderItem;
import orderservice.entity.OrderStatus;
import orderservice.service.OrderRepository;
import orderservice.service.component.CouponDiscountCalculator;
import orderservice.service.dto.CartOrderRequest;
import orderservice.service.dto.OrderRequest;
import orderservice.service.dto.OrderResponse;
import orderservice.service.dto.ProductCouponInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductServiceClient productClient;
    private final CouponServiceClient couponClient;
    private final PointServiceClient pointClient;
    private final CartServiceClient cartClient;
    private final SagaTransactionService sagaTransactionService;
    private final CompensationService compensationService;
    private final CouponDiscountCalculator couponDiscountCalculator;

    public OrderResponse order(OrderRequest request) {
        String sagaId = UUID.randomUUID().toString();
        log.info("Starting order processing - sagaId: {}, userId: {}", sagaId, request.getUserId());

        Order order = null;

        try {
            ProductResponse product = productClient.getProduct(request.getItemRequest().getProductId());
            order = createOrder(request, product);

            sagaTransactionService.createSaga(sagaId, order);

            decreaseStock(sagaId, request);
            useCoupon(sagaId, request, order);
            usePoint(sagaId, request, order);

            order.recalculateAmounts();
            orderRepository.save(order);
            sagaTransactionService.markCompleted(sagaId);

            log.info("Order completed successfully - sagaId: {}, orderId: {}", sagaId, order.getId());
            return OrderResponse.from(order);

        } catch (Exception e) {
            log.error("Order processing failed - sagaId: {}", sagaId);

            // 실패 처리
            handleOrderFailure(sagaId, order, e);

            throw new CompensationException("일반 주문 처리 실패: " + e.getMessage());
        }
    }

    @Transactional
    public OrderResponse cancel(Long orderId, Long userId) {
        String sagaId = UUID.randomUUID().toString();
        log.info("Cancelling order orderId={}, userId={}", orderId, userId);

        Order order = null;
        try {
            order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_ORDER));

            validateOrder(orderId, userId, order);
            restoreStock(sagaId, order);
            restoreCoupon(sagaId, order);
            restorePoint(sagaId, order);

            order.setStatus(OrderStatus.CANCELLED);
            log.info("Order successfully cancelled orderId={}", orderId);
            return OrderResponse.from(order);
        } catch (Exception e) {
            log.error("Order Cancel Processing Failed - sagaId: {}", sagaId, e.getMessage());
            handleOrderFailure(sagaId, order, e);

            throw new CompensationException("주문 취소 처리 실패: " + e.getMessage());
        }
    }

    private void restorePoint(String sagaId, Order order) {
        try {
            if (order.hasPointsUsed()) {
                Long pointAmount = order.getPointAmount().longValue();

                log.debug("Restoring points - sagaId: {}, pointAmount: {}P", sagaId, pointAmount);

                pointClient.earn(PointRequest.Earn.builder().amount(pointAmount).build());
                sagaTransactionService.markPointUsed(sagaId, pointAmount);

                log.info("Points restored - sagaId: {}, pointAmount: {}P", sagaId, pointAmount);
            } else {
                log.debug("No points to restore - sagaId: {}", sagaId);
            }
        } catch (Exception e) {
            throw new CompensationException("포인트 복구 실패: " + e.getMessage());
        }
    }

    private void restoreCoupon(String sagaId, Order order) {
        try {
            List<Long> usedCouponIds = new ArrayList<>();

            for (OrderItem item : order.getOrderItems()) {
                if (item.getCouponId() != null) {
                    usedCouponIds.add(item.getCouponId());
                }
            }

            if (!usedCouponIds.isEmpty()) {
                for (Long couponId : usedCouponIds) {
                    log.debug("Restoring coupon - sagaId: {}, couponId: {}", sagaId, couponId);

                    couponClient.cancelCoupon(couponId);
                    sagaTransactionService.markCouponUsed(sagaId, couponId);

                    log.info("Coupon restored - sagaId: {}, couponId: {}", sagaId, couponId);
                }
            }
        } catch (Exception e) {
            sagaTransactionService.markCouponUseFailed(sagaId, e.getMessage());
            throw new CompensationException("쿠폰 복구 실패: " + e.getMessage());
        }
    }

    private void restoreStock(String sagaId, Order order) {
        try {
            List<ProductOptionRequest.StockUpdate> stockUpdates = order.getOrderItems().stream()
                    .map(item -> ProductOptionRequest.StockUpdate.create(
                            item.getProductId(),
                            item.getProductOptionId(),
                            item.getQuantity()))
                    .collect(Collectors.toList());

            if (!stockUpdates.isEmpty()) {
                log.debug("Restoring stock after cancellation orderId={}, itemCount={}",
                        order.getId(), stockUpdates.size());

                productClient.increaseStock(stockUpdates);
                sagaTransactionService.markStockDecreased(sagaId); // 재고 복구 완료 표시

                log.info("Stock restored successfully - sagaId: {}, orderId: {}", sagaId, order.getId());
            }
        } catch (Exception e) {
            sagaTransactionService.markStockDecreaseFailed(sagaId, e.getMessage());
            throw new CompensationException("재고 복구 실패: " + e.getMessage());
        }
    }

    private void validateOrder(Long orderId, Long userId, Order order) {
        if (!order.getUserId().equals(userId)) {
            log.info("Cancel permission denied orderId={}, orderUserId={}, requestUserId={}",
                    orderId, order.getUserId(), userId);
            throw new CustomGlobalException(ErrorType.NO_PERMISSION_TO_CANCEL_ORDER);
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.debug("Order already cancelled orderId={}", orderId);
            throw new CustomGlobalException(ErrorType.ORDER_ALREADY_CANCELED);
        }
    }

    @Transactional
    public OrderResponse cartOrder(CartOrderRequest request) {
        String sagaId = UUID.randomUUID().toString();
        log.info("Starting cart order - sagaId: {}, userId: {}", sagaId, request.getUserId());

        Order order = null;
        try {
            CartResponse cartResponse = cartClient.getCart(request.getUserId());
            if (cartResponse.getItems().isEmpty()) {
                throw new CustomGlobalException(ErrorType.CART_EMPTY);
            }

            order = createOrderFromCart(request, cartResponse);
            order = orderRepository.save(order);
            log.info("Order created - orderId: {}", order.getId());

            sagaTransactionService.createSaga(sagaId, order);

            cartDecreaseStock(sagaId, cartResponse);
            cartUseCoupons(sagaId, order, request);
            cartUsePoints(sagaId, order, request);
            order.recalculateAmounts();
            orderRepository.save(order);

            cartClient.clearCart(request.getUserId());

            sagaTransactionService.markCompleted(sagaId);

            log.info("Cart order completed - sagaId: {}, orderId: {}", sagaId, order.getId());
            return OrderResponse.from(order);

        } catch (CustomGlobalException e) {
            log.error("Cart order failed - sagaId: {}, getMessage: {}", sagaId, e.getMessage());

            if (order != null) {
                handleOrderFailure(sagaId, order, e);
            }

            throw e;

        } catch (Exception e) {
            log.error("Cart order failed - sagaId: {}, getMessage: {}", sagaId, e.getMessage());

            // 실패 처리
            handleOrderFailure(sagaId, order, e);
            throw new CompensationException("장바구니 주문 실패: " + e.getMessage());
        }
    }

    private void cartUsePoints(String sagaId, Order order, CartOrderRequest request) {
        try {
            if (request.getPointAmount() != null && request.getPointAmount() > 0) {
                pointClient.use(new PointRequest.Use(request.getPointAmount()));
                order.applyPointDiscount(BigDecimal.valueOf(request.getPointAmount()));
                sagaTransactionService.markPointUsed(sagaId, request.getPointAmount());

                log.info("Points used successfully - sagaId: {}, amount: {}P",
                        sagaId, request.getPointAmount());
            }
        } catch (Exception e) {
            sagaTransactionService.markPointUseFailed(sagaId, e.getMessage());
            throw new CompensationException("포인트 사용 실패: " + e.getMessage());
        }
    }

    private void cartUseCoupons(String sagaId, Order order, CartOrderRequest request) {
        try {
            if (request.getProductCoupons() != null && !request.getProductCoupons().isEmpty()) {

                for (ProductCouponInfo couponInfo : request.getProductCoupons()) {
                    if (couponInfo.getCouponId() == null) {
                        continue;
                    }

                    CouponResponse.Response coupon = couponClient.getCoupon(couponInfo.getCouponId());
                    couponClient.useCoupon(couponInfo.getCouponId(), order.getId());

                    // 할인 금액 계산 (Domain Service 활용)
                    BigDecimal discountAmount = couponDiscountCalculator.calculateDiscountAmount(
                            coupon.getDiscountType(),
                            coupon.getDiscountValue(),
                            coupon.getMaximumDiscountAmount(),
                            getItemTotalPrice(order, couponInfo));

                    order.applyCouponToProduct(
                            couponInfo.getProductId(),
                            couponInfo.getProductOptionId(),
                            couponInfo.getCouponId(),
                            discountAmount
                    );

                    sagaTransactionService.markCouponUsed(sagaId, couponInfo.getCouponId());

                    log.info("Coupon applied - productId: {}, couponId: {}, discount: {}",
                            couponInfo.getProductId(), couponInfo.getCouponId(), discountAmount);
                }

                log.info("All coupons applied successfully - sagaId: {}", sagaId);
            }
        } catch (Exception e) {
            sagaTransactionService.markCouponUseFailed(sagaId, e.getMessage());
            throw new CompensationException("쿠폰 사용 실패: " + e.getMessage());
        }
    }

    private BigDecimal getItemTotalPrice(Order order, ProductCouponInfo couponInfo) {
        return order.getOrderItems().stream()
                .filter(item -> item.getProductId().equals(couponInfo.getProductId())
                        && item.getProductOptionId().equals(couponInfo.getProductOptionId()))
                .findFirst()
                .map(OrderItem::getTotalPrice)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("해당 상품을 찾을 수 없습니다 - productId: %d, optionId: %d",
                                couponInfo.getProductId(), couponInfo.getProductOptionId())));
    }

    private void cartDecreaseStock(String sagaId, CartResponse cartResponse) {
        try {
            List<ProductOptionRequest.StockUpdate> stockUpdates = cartResponse.getItems().stream()
                    .map(item -> ProductOptionRequest.StockUpdate.create(
                            item.getProductId(),
                            item.getOptionId(),
                            item.getQuantity()))
                    .collect(Collectors.toList());

            productClient.decreaseStock(stockUpdates);
            sagaTransactionService.markStockDecreased(sagaId);

            log.info("Stock decreased successfully - sagaId: {}", sagaId);

        } catch (Exception e) {
            sagaTransactionService.markStockDecreaseFailed(sagaId, e.getMessage());
            throw new CompensationException("재고 차감 실패: " + e.getMessage());
        }
    }

    private Order createOrderFromCart(CartOrderRequest request, CartResponse cartResponse) {
        Order order = Order.builder()
                .userId(request.getUserId())
                .status(OrderStatus.PENDING)
                .address(request.getAddress())
                .receiverName(request.getReceiverName())
                .receiverPhone(request.getReceiverPhone())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus("WAITING")
                .build();

        cartResponse.getItems().forEach(cartItem -> {
            OrderItem orderItem = OrderItem.builder()
                    .productId(cartItem.getProductId())
                    .productOptionId(cartItem.getOptionId())
                    .productName(cartItem.getProductName())
                    .optionName(cartItem.getOptionSize() + "/" + cartItem.getOptionColor())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(cartItem.getPrice())
                    .totalPrice(cartItem.getTotalPrice())
                    .build();
            order.addItem(orderItem);
        });

        BigDecimal totalAmount = order.getOrderItems().stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(totalAmount);

        return order;
    }

    @Transactional
    protected Order createOrder(OrderRequest request, ProductResponse product) {
        log.info("Creating order - userId: {}", request.getUserId());

        Order order = Order.create(request);
        OrderItem orderItem = OrderItem.create(request.getItemRequest(), product);
        order.addItem(orderItem);
        order.setTotalAmount(orderItem.getTotalPrice());

        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully - orderId: {}", savedOrder.getId());

        return savedOrder;
    }

    private void decreaseStock(String sagaId, OrderRequest request) {
        try {
            ProductOptionRequest.StockUpdate stockUpdate = ProductOptionRequest.StockUpdate.builder()
                    .productId(request.getItemRequest().getProductId())
                    .optionId(request.getItemRequest().getProductOptionId())
                    .quantity(request.getItemRequest().getQuantity())
                    .build();

            productClient.decreaseStock(List.of(stockUpdate));
            sagaTransactionService.markStockDecreased(sagaId);

            log.info("Stock decreased successfully - sagaId: {}", sagaId);

        } catch (Exception e) {
            sagaTransactionService.markStockDecreaseFailed(sagaId, e.getMessage());
            throw new CompensationException("재고 차감 실패: " + e.getMessage());
        }
    }

    private void useCoupon(String sagaId, OrderRequest request, Order order) {
        if (request.getCouponInfo() == null || request.getCouponInfo().getCouponId() == null) {
            log.info("Skipping coupon usage - no coupon provided");
            return;
        }

        try {
            Long couponId = request.getCouponInfo().getCouponId();
            CouponResponse.Response coupon = couponClient.getCoupon(couponId, request.getUserId());

            couponClient.useCoupon(couponId, order.getId());

            OrderItem orderItem = order.getOrderItems().get(0);
            BigDecimal discountAmount = couponDiscountCalculator.calculateDiscount(
                    orderItem.getTotalPrice(),
                    coupon.getDiscountType(),
                    BigDecimal.valueOf(coupon.getDiscountValue())
            );

            orderItem.applyCouponDiscount(couponId, discountAmount);
            orderRepository.save(order);

            log.info("Coupon applied to OrderItem - orderId: {}, couponId: {}, discountAmount: {}",
                    order.getId(), couponId, discountAmount);

            sagaTransactionService.markCouponUsed(sagaId, couponId);
            log.info("Coupon used successfully - sagaId: {}, couponId: {}", sagaId, couponId);

        } catch (Exception e) {
            sagaTransactionService.markCouponUseFailed(sagaId, e.getMessage());
            throw new CompensationException("쿠폰 사용 실패: " + e.getMessage());
        }
    }

    private void usePoint(String sagaId, OrderRequest request, Order order) {
        if (request.getPoint() == null || request.getPoint() <= 0) {
            log.info("Skipping point usage - no points to use");
            return;
        }

        try {
            pointClient.use(new PointRequest.Use(request.getPoint()));

            order.applyPointDiscount(BigDecimal.valueOf(request.getPoint()));
            orderRepository.save(order);

            sagaTransactionService.markPointUsed(sagaId, request.getPoint());
            log.info("Points used successfully - sagaId: {}, amount: {}P", sagaId, request.getPoint());

        } catch (Exception e) {
            throw new CompensationException("포인트 사용 실패: " + e.getMessage());
        }
    }

    /**
     * 주문 실패 처리 - 보상 트랜잭션은 CompensationService에 위임
     */
    private void handleOrderFailure(String sagaId, Order order, Exception e) {
        // 실패 상태 저장
        sagaTransactionService.markFailed(sagaId, e.getMessage());

        // 보상 트랜잭션 실행 (CompensationService에 위임)
        compensationService.executeCompensation(sagaId);

        // 주문 취소 처리
        if (order != null) {
            markOrderAsCancelled(order.getId());
        }
    }

    @Transactional
    private void markOrderAsCancelled(Long orderId) {
        Order order = getOrderById(orderId);
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Order marked as cancelled - orderId: {}", orderId);
    }

    private Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }
}