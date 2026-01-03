package couponservice.service.v3.dto;

import lombok.Builder;

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
