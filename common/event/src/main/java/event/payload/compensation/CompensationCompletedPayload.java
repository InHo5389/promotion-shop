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
public class CompensationCompletedPayload implements EventPayload {

    private String sagaId;
    private Long orderId;
    private Long userId;
    private String compensationType; // STOCK_RESTORE, COUPON_CANCEL, POINT_REFUND
    private boolean success;
    private String errorMessage;
    private Long timestamp;
}
