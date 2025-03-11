package orderservice.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;

    @NotEmpty(message = "주문 상품 목록은 비어있을 수 없습니다.")
    private List<OrderItemRequest> items;

    @NotBlank(message = "배송 주소는 필수입니다.")
    private String address;

    @NotBlank(message = "수령인 이름은 필수입니다.")
    private String receiverName;

    @NotBlank(message = "수령인 연락처는 필수입니다.")
    private String receiverPhone;

    @NotBlank(message = "결제 방법은 필수입니다.")
    private String paymentMethod;

    private ProductCouponInfo couponInfo;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Cancel{
        @NotNull(message = "사용자 ID는 필수입니다.")
        private Long userId;

        @NotNull(message = "주문 ID는 필수입니다.")
        private Long orderId;
    }
}
