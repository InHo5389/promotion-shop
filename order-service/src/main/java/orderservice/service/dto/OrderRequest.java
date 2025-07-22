package orderservice.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;

    private OrderItemRequest itemRequest;

    @NotBlank(message = "배송 주소는 필수입니다.")
    private String address;

    @NotBlank(message = "수령인 이름은 필수입니다.")
    private String receiverName;

    @NotBlank(message = "수령인 연락처는 필수입니다.")
    private String receiverPhone;

    @NotBlank(message = "결제 방법은 필수입니다.")
    private String paymentMethod;

    private ProductCouponInfo couponInfo;

    @Min(value = 100, message = "포인트는 최소 100원 이상 사용 가능합니다")
    private Long point;

    private Long timeSaleId;

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
