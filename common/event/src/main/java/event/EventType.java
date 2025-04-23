package event;

import event.payload.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@RequiredArgsConstructor
public enum EventType {
    ORDER_CREATED(OrderCreatedEventPayload.class, Topic.ORDER),

    COUPON_APPLIED(CouponAppliedEventPayload.class, Topic.COUPON_APPLIED),
    COUPON_CANCELED(CouponCanceledEventPayload.class, Topic.COUPON_CANCELED),

    STOCK_DECREASE(StockDecreasedEventPayload.class, Topic.PRODUCT_STOCK_DECREASE),
    STOCK_INCREASE(StockIncreasedEventPayload.class, Topic.PRODUCT_STOCK_INCREASE),

    POINT_USED(PointUsedEventPayload.class, Topic.POINT_USED),
    POINT_CANCELED(PointEarnedEventPayload.class, Topic.POINT_CANCELED),
    CART_CLEARED(CartClearedEventPayload.class, Topic.CART);

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

        public static final String ORDER = "order";
        public static final String ORDER_CANCEL = "order_cancel";
        public static final String PRODUCT_STOCK_INCREASE = "product_stock_increase";
        public static final String PRODUCT_STOCK_DECREASE = "product_stock_decrease";
        public static final String COUPON_APPLIED = "coupon_applied";
        public static final String COUPON_CANCELED = "coupon_cancel";
        public static final String CART = "cart";
        public static final String POINT_USED = "point_use";
        public static final String POINT_CANCELED = "point_cancel";
        public static final String PAYMENT = "payment";
    }
}
