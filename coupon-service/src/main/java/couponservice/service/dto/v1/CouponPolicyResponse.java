package couponservice.service.dto.v1;

import couponservice.entity.CouponPolicy;
import couponservice.entity.DiscountType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

public class CouponPolicyResponse {

    @Data
    @Builder
    public static class Create{
        private Long id;
        private String title;
        private String description;
        private DiscountType discountType;
        private Integer discountValue;
        private Integer minimumOrderAmount;
        private Integer maximumDiscountAmount;
        private Integer totalQuantity;
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        public static Create from(CouponPolicy couponPolicy){
            return Create.builder()
                    .id(couponPolicy.getId())
                    .title(couponPolicy.getTitle())
                    .description(couponPolicy.getDescription())
                    .discountType(couponPolicy.getDiscountType())
                    .discountValue(couponPolicy.getDiscountValue())
                    .minimumOrderAmount(couponPolicy.getMinimumOrderAmount())
                    .maximumDiscountAmount(couponPolicy.getMaximumDiscountAmount())
                    .totalQuantity(couponPolicy.getTotalQuantity())
                    .startTime(couponPolicy.getStartTime())
                    .endTime(couponPolicy.getEndTime())
                    .build();
        }
    }

    @Data
    @Builder
    public static class Response{
        private Long id;
        private String title;
        private String description;
        private DiscountType discountType;
        private Integer discountValue;
        private Integer minimumOrderAmount;
        private Integer maximumDiscountAmount;
        private Integer totalQuantity;
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        public static Response from(CouponPolicy couponPolicy){
            return Response.builder()
                    .id(couponPolicy.getId())
                    .title(couponPolicy.getTitle())
                    .description(couponPolicy.getDescription())
                    .discountType(couponPolicy.getDiscountType())
                    .discountValue(couponPolicy.getDiscountValue())
                    .minimumOrderAmount(couponPolicy.getMinimumOrderAmount())
                    .maximumDiscountAmount(couponPolicy.getMaximumDiscountAmount())
                    .totalQuantity(couponPolicy.getTotalQuantity())
                    .startTime(couponPolicy.getStartTime())
                    .endTime(couponPolicy.getEndTime())
                    .build();
        }
    }
}
