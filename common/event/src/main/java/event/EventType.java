package event;

import event.payload.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@RequiredArgsConstructor
public enum EventType {
    ORDER_CONFIRM(OrderConfirmPayload.class, Topic.ORDER_CONFIRM),
    STOCK_CONFIRM(StockConfirmPayload.class, Topic.STOCK_CONFIRM),
    COUPON_CONFIRM(CouponConfirmPayload.class, Topic.COUPON_CONFIRM),
    POINT_CONFIRM(PointConfirmPayload.class, Topic.POINT_CONFIRM),
    ORDER_COMPLETE(OrderCompletedPayload.class, Topic.ORDER_COMPLETED);


    private final Class<? extends EventPayload> payloadClass;
    private final String topic;

    public static EventType from(String type) {
        try {
            return valueOf(type);
        } catch (Exception e) {
            log.info("[EventType.from] type={}", type, e);
            return null;
        }
    }

    public static class Topic {
        public static final String ORDER_CONFIRM = "order-confirm";
        public static final String STOCK_CONFIRM = "stock-confirm";
        public static final String COUPON_CONFIRM = "coupon-confirm";
        public static final String POINT_CONFIRM = "point-confirm";
        public static final String ORDER_COMPLETED = "order-completed";
    }
}
