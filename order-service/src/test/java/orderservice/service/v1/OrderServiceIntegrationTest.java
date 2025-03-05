package orderservice.service.v1;

import orderservice.client.CartClient;
import orderservice.client.ProductClient;
import orderservice.client.dto.ProductResponse;
import orderservice.entity.Order;
import orderservice.entity.OrderStatus;
import orderservice.repository.OrderJpaRepository;
import orderservice.service.dto.OrderItemRequest;
import orderservice.service.dto.OrderRequest;
import orderservice.service.dto.OrderResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@Transactional
@SpringBootTest
@ActiveProfiles("test")
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @MockBean
    private ProductClient productClient;

    @MockBean
    private CartClient cartClient;

    @Test
    @DisplayName("동일한 상품 1개를 100명이 동시에 주문해도 재고는 정확히 100개 감소해야 한다")
    void concurrentOrder() throws InterruptedException {
        //given
        Long userId = 1L;
        Long productId = 1L;
        Long optionId = 1L;
        int quantity = 1;
        int numberOfThreads = 100;

        ProductResponse mockProduct = createMockProduct(productId, optionId);
        List<ProductResponse> products = List.of(mockProduct);

        given(productClient.getProducts(any())).willReturn(products);
        doNothing().when(productClient).decreaseStock(any());

        OrderRequest orderRequest = createOrderRequest(userId, productId, optionId, quantity);

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);

        //when
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    OrderResponse response = orderService.order(orderRequest);
                    if (response != null && response.getOrderId() != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // 예외 로깅
                    System.err.println("주문 처리 중 예외 발생: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        //then
        latch.await();
        executorService.shutdown();

        // Then
        // 1. 모든 주문이 성공했는지 확인
        assertThat(successCount.get()).isEqualTo(numberOfThreads);

        // 2. 주문이 데이터베이스에 올바르게 저장되었는지 확인
        List<Order> orders = orderJpaRepository.findByUserId(userId);
        assertThat(orders).hasSize(numberOfThreads);

        // 3. 각 주문의 상태가 PENDING인지 확인
        for (Order order : orders) {
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(order.getOrderItems()).hasSize(1);
            assertThat(order.getOrderItems().get(0).getProductId()).isEqualTo(productId);
            assertThat(order.getOrderItems().get(0).getProductOptionId()).isEqualTo(optionId);
            assertThat(order.getOrderItems().get(0).getQuantity()).isEqualTo(quantity);
        }

        // 4. ProductClient.decreaseStock이 정확히 호출되었는지 확인
        // 각 요청마다 한 번씩 호출되므로 총 호출 횟수는 numberOfThreads와 같아야 함
        verify(productClient, times(numberOfThreads)).decreaseStock(any());
    }

    private ProductResponse createMockProduct(Long productId, Long optionId) {
        ProductResponse.ProductOptionDTO option = ProductResponse.ProductOptionDTO.builder()
                .id(optionId)
                .size("M")
                .color("Black")
                .stockQuantity(10000) // 충분한 재고
                .build();

        // 상품 응답 생성 (빌더 패턴 사용)
        return ProductResponse.builder()
                .id(productId)
                .name("테스트 상품")
                .price(new BigDecimal("10000"))
                .status("ACTIVE")
                .options(List.of(option))
                .build();
    }

    private OrderRequest createOrderRequest(Long userId, Long productId, Long optionId, int quantity) {
        OrderItemRequest itemRequest = new OrderItemRequest().builder()
                .productId(productId)
                .productOptionId(optionId)
                .quantity(quantity)
                .build();

        return OrderRequest.builder()
                .userId(userId)
                .address("서울시 강남구")
                .receiverName("테스트 사용자")
                .receiverPhone("010-1234-5678")
                .paymentMethod("CARD")
                .items(List.of(itemRequest))
                .build();
    }
}