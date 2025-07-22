package event;

import event.payload.*;
import event.payload.compensation.CompensationCompletedPayload;
import event.payload.compensation.CouponCompensationRequestPayload;
import event.payload.compensation.PointCompensationRequestPayload;
import event.payload.compensation.StockCompensationRequestPayload;
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
    CART_CLEARED(CartClearedEventPayload.class, Topic.CART),

    STOCK_COMPENSATION_REQUEST(StockCompensationRequestPayload.class, Topic.STOCK_COMPENSATION_REQUEST),
    COUPON_COMPENSATION_REQUEST(CouponCompensationRequestPayload.class, Topic.COUPON_COMPENSATION_REQUEST),
    POINT_COMPENSATION_REQUEST(PointCompensationRequestPayload.class, Topic.POINT_COMPENSATION_REQUEST),
    COMPENSATION_COMPLETED(CompensationCompletedPayload.class, Topic.COMPENSATION_COMPLETED);

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

        // 보상 토픽들
        public static final String STOCK_COMPENSATION_REQUEST = "stock_compensation_request";
        public static final String COUPON_COMPENSATION_REQUEST = "coupon_compensation_request";
        public static final String POINT_COMPENSATION_REQUEST = "point_compensation_request";
        public static final String COMPENSATION_COMPLETED = "compensation_completed";
    }
}
