package event.payload;

import event.EventPayload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEventPayload implements EventPayload {

    private Long orderId;
    private Long userId;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal pointAmount;
    private BigDecimal finalAmount;
    private List<OrderItemData> items;
    private CouponInfo couponInfo;
    private List<CouponInfo> couponInfos;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemData {
        private Long productId;
        private Long productOptionId;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CouponInfo {
        private Long couponId;
        private Long productId;
        private Long productOptionId;
    }
}
