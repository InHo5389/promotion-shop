package orderservice.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class DiscountResult {

    private Long couponId;
    private BigDecimal discountAmount;
}
