package orderservice.client.dto;

import lombok.Builder;
import orderservice.service.dto.request.OrderRequest;

import java.util.List;

@Builder
public record CouponReserveRequest(
        Long orderId,
        Long userId,
        List<CouponItem> couponItems
){

    @Builder
    public record CouponItem(
            Long couponId,
            Long productOptionId,
            Integer productPrice
    ){
    }
}

