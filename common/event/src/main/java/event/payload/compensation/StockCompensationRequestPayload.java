package event.payload.compensation;

import event.EventPayload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockCompensationRequestPayload implements EventPayload {

    private String sagaId;
    private Long orderId;
    private Long userId;
    private Long productId;
    private Long productOptionId;
    private Integer quantity;
    private Long timestamp;
}
