package outboxmessagerelay;

import lombok.Getter;
import outboxmessagerelay.entity.Outbox;

// OutboxEvent - Spring 이벤트 발행을 위한 래퍼 클래스
@Getter
public class OutboxEvent {

    private Outbox outbox;

    public static OutboxEvent of(Outbox outbox) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.outbox = outbox;
        return outboxEvent;
    }
}
