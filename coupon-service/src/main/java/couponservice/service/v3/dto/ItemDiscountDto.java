package couponservice.service.v3.dto;

import lombok.Builder;

@Builder
public record ItemDiscountDto(
        Long productOptionId,
        Long couponId,
        Integer discountAmount
){
}
