package orderservice.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "compensation_registries")
@Getter
@NoArgsConstructor
public class CompensationRegistry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    private CompensationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompensationType type;

    @Builder
    public CompensationRegistry(Long orderId, CompensationType type) {
        this.orderId = orderId;
        this.status = CompensationStatus.PENDING;
        this.type = type;
    }

    public void markCompleted() {
        this.status = CompensationStatus.COMPLETED;
    }

    public enum CompensationStatus {
        PENDING,
        COMPLETED
    }

    public enum CompensationType {
        ORDER_CREATE_ROLLBACK,
        ORDER_CANCEL_PENDING,
        ORDER_CANCEL_CONFIRMED
    }
}
