package couponservice.service.v3.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record CouponReserveResponse(
        Integer totalDiscount,
        List<ItemDiscountDto> itemDiscounts
) {
}
