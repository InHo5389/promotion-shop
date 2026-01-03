package orderservice.repository;

import orderservice.entity.CompensationRegistry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompensationRegistryJpaRepository extends JpaRepository<CompensationRegistry, Long> {
    List<CompensationRegistry> findByStatusOrderByCreatedAtAsc(CompensationRegistry.CompensationStatus status);

    Optional<CompensationRegistry> findByOrderId(Long orderId);
}
