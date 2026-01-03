package orderservice.service.dto;

public record ItemDiscountDto (
        Long productOptionId,
        Long couponId,
        Integer discountAmount
){
}
