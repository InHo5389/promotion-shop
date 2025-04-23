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
public class StockDecreasedEventPayload implements EventPayload {
    private Long orderId;
    private Long userId;
    private List<StockItem> items;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockItem {
        private Long productId;
        private Long productOptionId;
        private Integer quantity;
    }
}
