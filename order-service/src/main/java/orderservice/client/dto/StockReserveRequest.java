package orderservice.client.dto;

import lombok.Builder;
import orderservice.service.dto.request.OrderRequest;

import java.util.List;

@Builder
public record StockReserveRequest(
        Long orderId,
        List<OrderItem> items
) {

    @Builder
    public record OrderItem(
            Long productId,
            Long productOptionId,
            Integer quantity,
            Integer productPrice
    ) {
        public static OrderItem from(OrderRequest.OrderItem source) {
            return OrderItem.builder()
                    .productId(source.productId())
                    .productOptionId(source.productOptionId())
                    .quantity(source.quantity())
                    .productPrice(source.productPrice())
                    .build();
        }
    }
}
