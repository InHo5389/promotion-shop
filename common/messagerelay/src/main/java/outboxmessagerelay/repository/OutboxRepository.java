package outboxmessagerelay.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import outboxmessagerelay.entity.Outbox;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {
    List<Outbox> findAllByCreatedAtLessThanEqualOrderByCreatedAtAsc(
            LocalDateTime before,
            Pageable pageable
    );
}
