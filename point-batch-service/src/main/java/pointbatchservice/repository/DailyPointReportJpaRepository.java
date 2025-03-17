package pointbatchservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pointbatchservice.entity.DailyPointReport;

public interface DailyPointReportJpaRepository extends JpaRepository<DailyPointReport, Long> {
}
