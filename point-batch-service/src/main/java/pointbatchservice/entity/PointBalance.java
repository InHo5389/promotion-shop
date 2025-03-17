package pointbatchservice.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pointbatchservice.common.BaseEntity;

@Getter
@Entity
@Table(name = "point_balances")
@NoArgsConstructor
public class PointBalance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private Long balance;

    @Version
    private Long version;

    @Builder
    public PointBalance(Long userId, Long balance) {
        this.userId = userId;
        this.balance = balance != null ? balance : 0L;
    }

    public static PointBalance create(Long userId) {
        PointBalance pointBalance = new PointBalance();
        pointBalance.userId = userId;
        pointBalance.balance = 0L;
        pointBalance.version = 0L;
        return pointBalance;
    }
}