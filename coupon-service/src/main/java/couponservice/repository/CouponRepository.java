package couponservice.repository;

import couponservice.entity.Coupon;
import couponservice.entity.CouponStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByIdAndUserId(Long id, Long userId);

    Optional<Coupon> findByCouponCode(String couponCode);

    Long countByCouponPolicyId(Long policyId);

    Page<Coupon> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, CouponStatus status, Pageable pageable);
}
