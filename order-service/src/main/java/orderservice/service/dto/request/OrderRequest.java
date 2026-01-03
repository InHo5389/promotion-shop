package orderservice.service.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class OrderRequest {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {

        @Min(value = 100, message = "포인트는 최소 100원 이상 사용 가능합니다")
        private Integer usePoint;

        @NotBlank(message = "배송 주소는 필수입니다.")
        private String address;

        @NotBlank(message = "수령인 이름은 필수입니다.")
        private String receiverName;

        @NotBlank(message = "수령인 연락처는 필수입니다.")
        @Pattern(regexp = "^01[0-9]-\\d{3,4}-\\d{4}$",
                message = "올바른 휴대폰 번호 형식이 아닙니다.")
        private String receiverPhone;

        @NotBlank(message = "결제 방법은 필수입니다.")
        private String paymentMethod;
    }

    @Builder
    public record OrderItem(
            Long productId,
            Long productOptionId,
            Integer quantity,
            Integer productPrice
    ) {
    }

    @Builder
    public record CouponItem(
            Long couponId,
            Long productOptionId,
            Long optionId,
            Integer productPrice
    ) {
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Cancel {
        @NotNull(message = "사용자 ID는 필수입니다.")
        private Long userId;

        @NotNull(message = "주문 ID는 필수입니다.")
        private Long orderId;
    }
}
