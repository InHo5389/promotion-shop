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
public class PointUsedEventPayload implements EventPayload {

    private Long orderId;
    private Long userId;
    private Long pointBalance;

}
