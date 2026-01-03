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

    @Column(nullable = false)
    private Long reservedBalance;

    @Version
    private Long version = 0L;

    @Builder
    public PointBalance(Long userId, Long balance) {
        this.userId = userId;
        this.balance = balance != null ? balance : 0L;
        this.reservedBalance = 0L;
    }

    public static PointBalance create(Long userId) {
        PointBalance pointBalance = new PointBalance();
        pointBalance.userId = userId;
        pointBalance.balance = 0L;
        pointBalance.reservedBalance = 0L;
        return pointBalance;
    }

    public void earn(Long amount) {
        if (amount <= 0) {
            throw new CustomGlobalException(ErrorType.POINT_BALANCE_MUST_BE_POSITIVE);
        }

        this.balance += amount;
    }

    public void reserve(Long amount) {
        Long availableBalance = this.balance - this.reservedBalance;
        if (availableBalance < amount) {
            throw new CustomGlobalException(ErrorType.NOT_ENOUGH_POINT_BALANCE);
        }
        this.reservedBalance += amount;
    }

    public void confirmReservation(Long amount) {
        if (this.reservedBalance < amount) {
            throw new CustomGlobalException(ErrorType.INVALID_POINT_RESERVATION);
        }
        this.balance -= amount;
        this.reservedBalance -= amount;
    }

    public void cancelReservation(Long amount) {
        if (this.reservedBalance < amount) {
            throw new CustomGlobalException(ErrorType.INVALID_POINT_RESERVATION);
        }
        this.reservedBalance -= amount;
    }

    public void rollbackConfirmation(Long amount) {
        this.balance += amount;
    }

    public void use(Long amount) {
        if (this.balance < amount) {
            throw new CustomGlobalException(ErrorType.NOT_ENOUGH_POINT_BALANCE);
        }

        this.balance -= amount;
    }

    public void refund(Long amount) {
        this.balance += amount;
    }

    public void reserveRefund(Long amount) {
        this.reservedBalance += amount;
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
