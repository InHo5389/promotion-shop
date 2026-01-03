package productservice.service.dto;

import lombok.Builder;

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
    }
}
