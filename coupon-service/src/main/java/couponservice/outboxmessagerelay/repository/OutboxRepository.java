package couponservice.outboxmessagerelay.repository;

import couponservice.outboxmessagerelay.entity.Outbox;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {
    List<Outbox> findAllByCreatedAtLessThanEqualOrderByCreatedAtAsc(
            LocalDateTime before,
            Pageable pageable
    );
}
