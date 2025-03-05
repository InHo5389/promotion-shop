package orderservice.service.dto;

import jakarta.validation.Valid;
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
@Valid
public class CartOrderRequest {
    @NotNull(message = "사용자 ID는 필수입니다")
    private Long userId;

    @NotBlank(message = "배송지 주소는 필수입니다")
    private String address;

    @NotBlank(message = "수령인 이름은 필수입니다")
    private String receiverName;

    @NotBlank(message = "수령인 연락처는 필수입니다")
    private String receiverPhone;

    @NotBlank(message = "결제 방법은 필수입니다")
    private String paymentMethod;
}
