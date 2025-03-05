package orderservice.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequest {
    @NotNull(message = "상품 ID는 필수입니다.")
    private Long productId;

    @NotNull(message = "상품 옵션 ID는 필수입니다.")
    private Long productOptionId;

    @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
    private int quantity;
}
