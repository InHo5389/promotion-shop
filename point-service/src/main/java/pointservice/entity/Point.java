package pointservice.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pointservice.common.BaseEntity;
import pointservice.common.exception.CustomGlobalException;
import pointservice.common.exception.ErrorType;

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

    @Version
    private Long version;

    @Builder
    public Point(Long userId, Long amount, PointType type, Long balanceSnapshot, PointBalance pointBalance) {
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.balanceSnapshot = balanceSnapshot;
        this.pointBalance = pointBalance;
    }

    public static Point create(Long userId, Long amount, PointType type, Long balanceSnapshot, PointBalance pointBalance){
        Point point = new Point();
        point.userId = userId;
        point.amount = amount;
        point.type = type;
        point.balanceSnapshot = balanceSnapshot;
        point.pointBalance = pointBalance;
        point.version = 0L;
        return point;
    }

    public void cancel() {
        if (this.type == PointType.CANCELED) {
            throw new CustomGlobalException(ErrorType.ALREADY_CANCELED_POINT);
        }

        if (this.pointBalance == null) {
            throw new CustomGlobalException(ErrorType.NOT_FOUND_POINT_BALANCE);
        }

        Long currentBalance = this.pointBalance.getBalance();

        if (this.type == PointType.EARNED) {
            if (currentBalance < this.amount) {
                throw new CustomGlobalException(ErrorType.NOT_ENOUGH_POINT_BALANCE);
            }
            this.pointBalance.decreaseBalance(this.amount);
        } else if (this.type == PointType.USED) {
            this.pointBalance.increaseBalance(this.amount);
        } else {
            throw new CustomGlobalException(ErrorType.INVALID_POINT_TYPE);
        }

        this.type = PointType.CANCELED;
    }
}
