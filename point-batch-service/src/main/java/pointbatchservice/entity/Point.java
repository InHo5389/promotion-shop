package pointbatchservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pointbatchservice.common.BaseEntity;

@Getter
@Entity
@Table(name = "points")
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}
