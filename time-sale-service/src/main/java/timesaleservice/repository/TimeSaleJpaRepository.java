package timesaleservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import timesaleservice.entity.TimeSale;

public interface TimeSaleJpaRepository extends JpaRepository<TimeSale, Long> {
}
