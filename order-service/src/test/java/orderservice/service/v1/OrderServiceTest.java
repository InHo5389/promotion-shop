package orderservice.service.v1;

import orderservice.client.ProductClient;
import orderservice.client.dto.ProductOptionRequest;
import orderservice.client.dto.ProductResponse;
import orderservice.common.exception.CustomGlobalException;
import orderservice.common.exception.ErrorType;
import orderservice.entity.Order;
import orderservice.entity.OrderItem;
import orderservice.entity.OrderStatus;
import orderservice.service.OrderRepository;
import orderservice.service.dto.OrderItemRequest;
import orderservice.service.dto.OrderRequest;
import orderservice.service.dto.OrderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private ProductClient productClient;

    @Mock
    private OrderRepository orderRepository;

    private OrderRequest orderRequest;
    private List<ProductResponse> productResponses;
    private Order mockOrder;

    @BeforeEach
    void setUp() {
        List<ProductResponse.ProductOptionDTO> options1 = Collections.singletonList(
                ProductResponse.ProductOptionDTO.builder()
                        .id(1L)
                        .size("M")
                        .color("Black")
                        .additionalPrice(BigDecimal.ZERO)
                        .stockQuantity(10)
                        .build()
        );

        List<ProductResponse.ProductOptionDTO> options2 = Collections.singletonList(
                ProductResponse.ProductOptionDTO.builder()
                        .id(2L)
                        .size("L")
                        .color("White")
                        .additionalPrice(BigDecimal.valueOf(2000))
                        .stockQuantity(5)
                        .build()
        );

        List<OrderItemRequest> items = Arrays.asList(
                new OrderItemRequest(1L, 1L, 2),
                new OrderItemRequest(2L, 2L, 1)
        );

        orderRequest = new OrderRequest(
                1L,
                items,
                "서울시 강남구 테헤란로 123",
                "홍길동",
                "010-1234-5678",
                "CARD"
        );

        productResponses = Arrays.asList(
                ProductResponse.builder()
                        .id(1L)
                        .name("상품1")
                        .price(BigDecimal.valueOf(10000))
                        .image("image1.jpg")
                        .status("ACTIVE")
                        .categoryId(1L)
                        .categoryName("의류")
                        .options(options1)
                        .build(),
                ProductResponse.builder()
                        .id(2L)
                        .name("상품2")
                        .price(BigDecimal.valueOf(20000))
                        .image("image2.jpg")
                        .status("ACTIVE")
                        .categoryId(2L)
                        .categoryName("신발")
                        .options(options2)
                        .build()
        );

        // Order 모의 객체 생성
        mockOrder = new Order(1L, orderRequest.getUserId(), BigDecimal.valueOf(42000), OrderStatus.PENDING,
                new ArrayList<>(), orderRequest.getAddress(), orderRequest.getReceiverName(),
                orderRequest.getReceiverPhone(), orderRequest.getPaymentMethod(), "");

        // OrderItem 추가
        OrderItem item1 = OrderItem.create(items.get(0), productResponses.get(0));
        OrderItem item2 = OrderItem.create(items.get(1), productResponses.get(1));
        mockOrder.addItem(item1);
        mockOrder.addItem(item2);
    }

    @Test
    @DisplayName("주문 생성 성공 테스트")
    void orderSuccess() {
        //given
        given(productClient.getProducts(any())).willReturn(productResponses);
        given(orderRepository.save(any(Order.class))).willReturn(mockOrder);
        //when
        OrderResponse response = orderService.order(orderRequest);
        //then
        verify(productClient).getProducts(any());
        verify(productClient).decreaseStock(any());
        verify(orderRepository).save(any(Order.class));

        assertThat(response).isNotNull();
        assertThat(response.getTotalAmount()).isEqualTo(BigDecimal.valueOf(42000));
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getAddress()).isEqualTo(orderRequest.getAddress());
        assertThat(response.getReceiverName()).isEqualTo(orderRequest.getReceiverName());
        assertThat(response.getReceiverPhone()).isEqualTo(orderRequest.getReceiverPhone());
        assertThat(response.getPaymentMethod()).isEqualTo(orderRequest.getPaymentMethod());
    }


    @Test
    @DisplayName("재고 업데이트 검증 테스트")
    void verifyStockUpdateRequests() {
        // Given
        given(productClient.getProducts(any())).willReturn(productResponses);
        given(orderRepository.save(any(Order.class))).willReturn(mockOrder);

        // 재고 업데이트 요청 캡처를 위한 ArgumentCaptor
        ArgumentCaptor<List<ProductOptionRequest.StockUpdate>> stockUpdateCaptor =
                ArgumentCaptor.forClass((Class) List.class);

        // When
        orderService.order(orderRequest);

        // Then
        verify(productClient).decreaseStock(stockUpdateCaptor.capture());
        List<ProductOptionRequest.StockUpdate> capturedUpdates = stockUpdateCaptor.getValue();

        assertThat(capturedUpdates).hasSize(2);
        assertThat(capturedUpdates.get(0).getProductId()).isEqualTo(1L);
        assertThat(capturedUpdates.get(0).getOptionId()).isEqualTo(1L);
        assertThat(capturedUpdates.get(0).getQuantity()).isEqualTo(2);

        assertThat(capturedUpdates.get(1).getProductId()).isEqualTo(2L);
        assertThat(capturedUpdates.get(1).getOptionId()).isEqualTo(2L);
        assertThat(capturedUpdates.get(1).getQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("주문 취소 요청 객체 사용 테스트")
    void cancelOrderWithCancelRequestObject() {
        // Given
        OrderRequest.Cancel cancelRequest = new OrderRequest.Cancel(1L, 1L);

        given(orderRepository.findById(cancelRequest.getOrderId())).willReturn(Optional.of(mockOrder));

        // When
        OrderResponse response = orderService.cancel(cancelRequest.getOrderId(), cancelRequest.getUserId());

        // Then
        verify(productClient).increaseStock(any());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    private Order createMockOrder(Long orderId, OrderRequest orderRequest, BigDecimal totalAmount, OrderStatus status) {
        return new Order(
                orderId,
                orderRequest.getUserId(),
                totalAmount,
                status,
                new ArrayList<>(), // 수정 가능한 빈 리스트 사용
                orderRequest.getAddress(),
                orderRequest.getReceiverName(),
                orderRequest.getReceiverPhone(),
                orderRequest.getPaymentMethod(),
                ""
        );
    }

    @Test
    @DisplayName("주문 찾을 수 없음 예외 테스트")
    void cancelOrderNotFound() {
        // Given
        Long orderId = 999L;
        Long userId = 1L;

        given(orderRepository.findById(orderId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(()->orderService.cancel(orderId, userId))
                .isInstanceOf(CustomGlobalException.class)
                .hasMessage(ErrorType.NOT_FOUND_ORDER.getMessage());
    }

    @Test
    @DisplayName("주문 취소 권한 없음 예외 테스트")
    void cancelOrderNoPermission() {
        // Given
        Long orderId = 1L;
        Long userId = 999L; // 다른 사용자 ID

        // userId가 1L인 주문을 모킹
        Order order = createMockOrder(orderId, orderRequest, BigDecimal.valueOf(42000), OrderStatus.PENDING);
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        // When & Then
        assertThatThrownBy(()->orderService.cancel(orderId, userId))
                .isInstanceOf(CustomGlobalException.class)
                .hasMessage(ErrorType.NO_PERMISSION_TO_CANCEL_ORDER.getMessage());
    }

    @Test
    @DisplayName("이미 취소된 주문 예외 테스트")
    void cancelAlreadyCanceledOrder() {
        // Given
        Long orderId = 1L;
        Long userId = 1L;

        Order canceledOrder = createMockOrder(orderId, orderRequest, BigDecimal.valueOf(42000), OrderStatus.CANCELLED);
        given(orderRepository.findById(orderId)).willReturn(Optional.of(canceledOrder));

        // When & Then
        assertThatThrownBy(()->orderService.cancel(orderId, userId))
                .isInstanceOf(CustomGlobalException.class)
                .hasMessage(ErrorType.ORDER_ALREADY_CANCELED.getMessage());
    }

    @Test
    @DisplayName("배송 중인 주문 취소 예외 테스트")
    void cancelShippedOrder() {
        // Given
        Long orderId = 1L;
        Long userId = 1L;

        Order shippedOrder = createMockOrder(orderId, orderRequest, BigDecimal.valueOf(42000), OrderStatus.SHIPPED);
        given(orderRepository.findById(orderId)).willReturn(Optional.of(shippedOrder));

        // When & Then
        assertThatThrownBy(()->orderService.cancel(orderId, userId))
                .isInstanceOf(CustomGlobalException.class)
                .hasMessage(ErrorType.ORDER_CANNOT_BE_CANCELED.getMessage());
    }

    @Test
    @DisplayName("옵션 가격 추가된 상품 가격 계산 테스트")
    void calculateTotalPriceWithAdditionalPrice() {
        // Given
        given(productClient.getProducts(any())).willReturn(productResponses);
        given(orderRepository.save(any(Order.class))).willReturn(mockOrder);

        // When
        OrderResponse response = orderService.order(orderRequest);

        // Then
        assertThat(response.getTotalAmount()).isEqualTo(BigDecimal.valueOf(42000));
        // 상품1: 10000 * 2 = 20000
        // 상품2: 20000 + 2000(추가 가격) = 22000
        // 총합: 42000
    }
}