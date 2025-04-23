package event.payload;

import event.EventPayload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponAppliedEventPayload implements EventPayload {

    private Long orderId;
    private Long userId;
    private Long couponId;
    private Long productId;
    private Long productOptionId;
}
