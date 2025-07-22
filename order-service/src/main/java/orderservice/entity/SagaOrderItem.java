package orderservice.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Embeddable
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaOrderItem {
    private Long productId;
    private Long productOptionId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private Long cartItemId;
}
