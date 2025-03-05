package orderservice.service.v1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.client.CartClient;
import orderservice.client.ProductClient;
import orderservice.client.dto.*;
import orderservice.common.exception.CustomGlobalException;
import orderservice.common.exception.ErrorType;
import orderservice.entity.Order;
import orderservice.entity.OrderItem;
import orderservice.entity.OrderStatus;
import orderservice.service.OrderRepository;
import orderservice.service.dto.CartOrderRequest;
import orderservice.service.dto.OrderItemRequest;
import orderservice.service.dto.OrderRequest;
import orderservice.service.dto.OrderResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final ProductClient productClient;
    private final CartClient cartClient;
    private final OrderRepository orderRepository;

    @Transactional
    public OrderResponse order(OrderRequest request) {
        log.info("Creating order for user {}", request.getUserId());

        // 1. 모든 상품 ID 수집
        List<Long> productIds = request.getItems().stream()
                .map(OrderItemRequest::getProductId)
                .toList();

        // 2. 상품 정보 일괄 조회
        List<ProductResponse> products = productClient.getProducts(new ProductRequest.ReadProductIds(productIds));
        Map<Long, ProductResponse> productMap = products.stream()
                .collect(Collectors.toMap(
                        ProductResponse::getId,
                        p -> p,
                        (existing, replacement) -> existing));

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

        List<ProductOptionRequest.StockUpdate> stockUpdates = new ArrayList<>();

        for (OrderItem orderItem : order.getOrderItems()) {
            stockUpdates.add(ProductOptionRequest.StockUpdate.create(
                    orderItem.getProductId(),
                    orderItem.getProductOptionId(),
                    orderItem.getQuantity()
            ));
        }

        // 재고 일괄 증가 (1회 API 호출)
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

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<ProductOptionRequest.StockUpdate> stockUpdates = new ArrayList<>();

        for (CartProductResponse cartItem : cartResponse.getItems()) {

            OrderItem orderItem = OrderItem.builder()
                    .productId(cartItem.getProductId())
                    .productOptionId(cartItem.getOptionId())
                    .productName(cartItem.getProductName())
                    .optionName(String.format("%s / %s", cartItem.getOptionSize(), cartItem.getOptionColor()))
                    .quantity(cartItem.getQuantity())
                    .price(cartItem.getPrice())
                    .totalPrice(cartItem.getTotalPrice())
                    .build();

            order.addItem(orderItem);
            totalAmount = totalAmount.add(orderItem.getTotalPrice());

            stockUpdates.add(ProductOptionRequest.StockUpdate.create(
                    cartItem.getProductId(),
                    cartItem.getOptionId(),
                    cartItem.getQuantity()
            ));
        }

        productClient.decreaseStock(stockUpdates);

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);

        cartClient.clearCart(request.getUserId());

        return OrderResponse.from(savedOrder);
    }
    private void validateCartItems(List<CartProductResponse> cartItems) {
        // 상품 ID 수집
        List<Long> productIds = cartItems.stream()
                .map(CartProductResponse::getProductId)
                .distinct()
                .toList();

        // 상품 정보 일괄 조회
        List<ProductResponse> products = productClient.getProducts(
                new ProductRequest.ReadProductIds(productIds));

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
