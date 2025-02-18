package couponservice.service.dto.v1;

import couponservice.entity.Coupon;
import couponservice.entity.CouponPolicy;
import couponservice.entity.CouponStatus;
import couponservice.entity.DiscountType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

public class CouponResponse {

    @Data
    @Builder
    public static class Response{
        private Long id;
        private Long userId;
        private String couponCode;
        private String description;
        private DiscountType discountType;
        private Integer discountValue;
        private Integer minimumOrderAmount;
        private Integer maximumDiscountAmount;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private CouponStatus couponStatus;
        private Long orderId;
        private LocalDateTime usedAt;

        public static CouponResponse.Response from(Coupon coupon){
            CouponPolicy couponPolicy = coupon.getCouponPolicy();
            return Response.builder()
                    .id(coupon.getId())
                    .userId(coupon.getUserId())
                    .couponCode(coupon.getCouponCode())
                    .description(couponPolicy.getDescription())
                    .discountType(couponPolicy.getDiscountType())
                    .discountValue(couponPolicy.getDiscountValue())
                    .minimumOrderAmount(couponPolicy.getMinimumOrderAmount())
                    .maximumDiscountAmount(couponPolicy.getMaximumDiscountAmount())
                    .startTime(couponPolicy.getStartTime())
                    .endTime(couponPolicy.getEndTime())
                    .couponStatus(coupon.getStatus())
                    .orderId(coupon.getOrderId())
                    .usedAt(coupon.getUsedAt())
                    .build();
        }
    }
}
