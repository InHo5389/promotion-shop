package couponservice.repository;

import couponservice.entity.Coupon;
import couponservice.entity.CouponStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByIdAndUserId(Long id, Long userId);

    Optional<Coupon> findByCouponCode(String couponCode);

    Long countByCouponPolicyId(Long policyId);

    Page<Coupon> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, CouponStatus status, Pageable pageable);

    @Query("SELECT c FROM Coupon c JOIN FETCH c.couponPolicy WHERE c.id = :id")
    Optional<Coupon> findByIdWithPolicy(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c JOIN FETCH c.couponPolicy WHERE c.id = :id")
    Optional<Coupon> findByIdWithPolicyForUpdate(@Param("id") Long id);
}
