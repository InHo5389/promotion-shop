package outboxmessagerelay.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Outbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String topic;

    @Column(columnDefinition = "TEXT")
    private String payload;
    private LocalDateTime createdAt;

    public static Outbox create(String topic, String payload) {
        Outbox outbox = new Outbox();
        outbox.topic = topic;
        outbox.payload = payload;
        outbox.createdAt = LocalDateTime.now();
        return outbox;
    }
}
