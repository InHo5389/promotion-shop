package orderservice.service.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponCancelEvent {
    private Long orderId;

    @Builder.Default
    private LocalDateTime eventTime = LocalDateTime.now();

    @Builder.Default
    private String eventId = java.util.UUID.randomUUID().toString();
}
