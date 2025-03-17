package pointbatchservice.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pointbatchservice.entity.PointBalance;

import java.util.Optional;

public interface PointBalanceJpaRepository extends JpaRepository<PointBalance, Long> {

    @Lock(LockModeType.OPTIMISTIC)
    Optional<PointBalance> findByUserId(Long userId);

    @Query("SELECT p FROM PointBalance p WHERE p.userId = :userId")
    Optional<PointBalance> findByUserIdNoLock(@Param("userId") Long userId);
}
