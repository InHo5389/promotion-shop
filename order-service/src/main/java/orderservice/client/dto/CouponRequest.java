package orderservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class CouponRequest {


    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Use{
        private Long orderId;
    }
}
