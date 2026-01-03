package orderservice.client.dto;

import lombok.Builder;
import orderservice.service.dto.ItemDiscountDto;

import java.util.List;

@Builder
public record CouponReserveResponse(
        Integer totalDiscount,
        List<ItemDiscountDto> itemDiscounts
) {
}
