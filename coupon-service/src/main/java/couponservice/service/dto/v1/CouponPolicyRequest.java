package couponservice.service.dto.v1;

import couponservice.entity.CouponPolicy;
import couponservice.entity.DiscountType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class CouponPolicyRequest {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Create {
        @NotBlank(message = "쿠폰 정책 제목은 필수입니다.")
        private String title;

        private String description;

        @NotNull(message = "할인 타입은 필수입니다.")
        private DiscountType discountType;

        @NotNull(message = "할인 값은 필수입니다.")
        @Min(value = 100, message = "할인 값은 100 이상이여야 합니다.")
        private Integer discountValue;

        @NotNull(message = "최소 주문 금액은 필수입니다.")
        @Min(value = 0, message = "최소 주문 금액은 0 이상이여야 합니다.")
        private Integer minimumOrderAmount;

        @NotNull(message = "최대 할인 금액은 필수입니다.")
        @Min(value = 1, message = "최대 할인 금액은 1 이상이여야 합니다.")
        private Integer maximumDiscountAmount;

        @NotNull(message = "총 수량은 필수입니다.")
        @Min(value = 1, message = "총 수량은 1 이상이어야 합니다.")
        private Integer totalQuantity;

        @NotNull(message = "사용 시작 기간은 필수입니다.")
        private LocalDateTime startTime;

        @NotNull(message = "사용 종료 기간은 필수입니다.")
        private LocalDateTime endTime;

        public CouponPolicy toEntity(){
            return CouponPolicy.builder()
                    .title(title)
                    .description(description)
                    .discountType(discountType)
                    .discountValue(discountValue)
                    .minimumOrderAmount(minimumOrderAmount)
                    .maximumDiscountAmount(maximumDiscountAmount)
                    .totalQuantity(totalQuantity)
                    .startTime(startTime)
                    .endTime(endTime)
                    .build();
        }
    }
}
