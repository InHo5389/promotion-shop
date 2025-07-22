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
public class CouponCompensationRequestPayload implements EventPayload {

    private String sagaId;
    private Long orderId;
    private Long userId;
    private Long couponId;
    private Long timestamp;
}
