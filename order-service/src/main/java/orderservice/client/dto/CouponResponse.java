package orderservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class CouponResponse {
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long userId;
        private String couponCode;
        private String description;
        private String  discountType;
        private Integer discountValue;
        private Integer minimumOrderAmount;
        private Integer maximumDiscountAmount;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String couponStatus;
        private Long orderId;
        private LocalDateTime usedAt;
    }
}
