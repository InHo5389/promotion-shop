package orderservice.service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponReserveResponse {
    private Map<Long, Integer> discounts;
    private Integer totalDiscount;
}
