package event.payload;

import event.EventPayload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponCanceledEventPayload implements EventPayload {

    private Long orderId;
    private Long userId;
    private Long couponId;
    private Long productId;
    private Long productOptionId;
    private List<CouponInfo> couponInfos;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CouponInfo{
        private Long couponId;
        private Long productId;
        private Long productOptionId;
    }
}
