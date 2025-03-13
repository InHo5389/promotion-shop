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

    public void earn(Long amount) {
        if (amount <= 0) {
            throw new CustomGlobalException(ErrorType.POINT_BALANCE_MUST_BE_POSITIVE);
        }

        this.balance += amount;
    }

    public void use(Long amount) {
        if (this.balance < amount) {
            throw new CustomGlobalException(ErrorType.NOT_ENOUGH_POINT_BALANCE);
        }

        this.balance -= amount;
    }

    public void setBalance(Long balance) {
        if (balance == null || balance < 0) {
            throw new CustomGlobalException(ErrorType.POINT_BALANCE_INVALID);
        }
        this.balance = balance;
    }

    public void increaseBalance(Long amount) {
        this.balance += amount;
    }

    public void decreaseBalance(Long amount) {
        this.balance -= amount;
    }
}
