package pointservice.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import pointservice.common.BaseEntity;
import pointservice.common.exception.CustomGlobalException;
import pointservice.common.exception.ErrorType;

@Slf4j
@Getter
@Entity
@Table(name = "points")
@NoArgsConstructor
public class Point extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PointType type;

    @Column(nullable = false)
    private Long balanceSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "point_balance_id")
    private PointBalance pointBalance;

    @Setter
    private Long orderId;

    @Builder
    public Point(Long userId, Long amount, PointType type, Long balanceSnapshot, PointBalance pointBalance) {
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.balanceSnapshot = balanceSnapshot;
        this.pointBalance = pointBalance;
    }

    public static Point create(Long userId, Long amount, PointType type, Long balanceSnapshot, PointBalance pointBalance) {
        Point point = new Point();
        point.userId = userId;
        point.amount = amount;
        point.type = type;
        point.balanceSnapshot = balanceSnapshot;
        point.pointBalance = pointBalance;
        return point;
    }

    public void cancel() {
        if (this.type == PointType.CANCELED) {
            log.info("포인트 취소 불가 - 이미 취소됨 - userId: {}, pointId: {}, amount: {}P",
                    this.userId, this.id, this.amount);
            throw new CustomGlobalException(ErrorType.ALREADY_CANCELED_POINT);
        }

        if (this.pointBalance == null) {
            throw new CustomGlobalException(ErrorType.NOT_FOUND_POINT_BALANCE);
        }

        Long currentBalance = this.pointBalance.getBalance();

        if (this.type == PointType.EARNED) {
            if (currentBalance < this.amount) {
                log.info("포인트 취소 불가 - 잔액 부족 - userId: {}, pointId: {}, 취소 필요 금액: {}P, 현재 잔액: {}P",
                        this.userId, this.id, this.amount, currentBalance);
                throw new CustomGlobalException(ErrorType.NOT_ENOUGH_POINT_BALANCE);
            }

            this.pointBalance.decreaseBalance(this.amount);
            log.info("적립 포인트 취소 완료 - userId: {}, pointId: {}, amount: {}P, 이전 잔액: {}P, 갱신 잔액: {}P",
                    this.userId, this.id, this.amount, currentBalance, this.pointBalance.getBalance());
        } else if (this.type == PointType.USED) {
            this.pointBalance.increaseBalance(this.amount);
            log.info("사용 포인트 취소 완료 - userId: {}, pointId: {}, amount: {}P, 이전 잔액: {}P, 갱신 잔액: {}P",
                    this.userId, this.id, this.amount, currentBalance, this.pointBalance.getBalance());
        } else {
            throw new CustomGlobalException(ErrorType.INVALID_POINT_TYPE);
        }

        this.type = PointType.CANCELED;
    }
}
