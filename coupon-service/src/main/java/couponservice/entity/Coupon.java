package couponservice.entity;

import couponservice.common.BaseEntity;
import couponservice.common.exception.CustomGlobalException;
import couponservice.common.exception.ErrorType;
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
@Table(name = "coupons")
public class Coupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private CouponPolicy couponPolicy;

    private Long userId;

    private String couponCode;

    @Enumerated(EnumType.STRING)
    private CouponStatus status;

    private Long orderId;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;

    @Version
    private Long version = 0L;

    public static Coupon create(CouponPolicy couponPolicy, Long currentUserId, String couponCode) {
        return Coupon.builder()
                .couponPolicy(couponPolicy)
                .userId(currentUserId)
                .couponCode(couponCode)
                .status(CouponStatus.AVAILABLE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void reserve(Long orderId) {
        if (this.status != CouponStatus.AVAILABLE) {
            throw new CustomGlobalException(ErrorType.COUPON_NOT_AVAILABLE);
        }
        if (isExpired()) {
            throw new CustomGlobalException(ErrorType.COUPON_EXPIRED);
        }
        this.status = CouponStatus.RESERVED;
        this.orderId = orderId;
    }

    public void confirmReservation() {
        if (this.status != CouponStatus.RESERVED) {
            throw new CustomGlobalException(ErrorType.COUPON_NOT_RESERVED);
        }
        this.status = CouponStatus.USED;
        this.usedAt = LocalDateTime.now();
    }

    public void cancelReservation() {
        if (this.status != CouponStatus.RESERVED) {
            throw new CustomGlobalException(ErrorType.COUPON_NOT_RESERVED);
        }
        this.status = CouponStatus.AVAILABLE;
        this.orderId = null;
    }

    public void rollbackConfirmation() {
        if (this.status != CouponStatus.USED) {
            throw new RuntimeException("사용된 쿠폰이 아닙니다.");
        }
        this.status = CouponStatus.AVAILABLE;
        this.usedAt = null;
    }


    public void cancel(){
        if (this.status != CouponStatus.USED){
            throw new CustomGlobalException(ErrorType.COUPON_NOT_USED);
        }

        this.status = CouponStatus.CANCELED;
        this.orderId = null;
        this.usedAt = null;
    }

    private boolean isExpired() {
        LocalDateTime now = LocalDateTime.now();
        return now.isBefore(couponPolicy.getStartTime()) || now.isAfter(couponPolicy.getEndTime());
    }
}
