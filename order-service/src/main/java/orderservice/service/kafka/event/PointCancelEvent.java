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
public class PointCancelEvent {
    private Long orderId;
    private Long userId;

    @Builder.Default
    private LocalDateTime eventTime = LocalDateTime.now();

    @Builder.Default
    private String eventId = java.util.UUID.randomUUID().toString();
}
