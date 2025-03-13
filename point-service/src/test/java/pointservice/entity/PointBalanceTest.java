package pointservice.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import pointservice.common.exception.CustomGlobalException;
import pointservice.common.exception.ErrorType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PointBalanceTest {

    @Test
    @DisplayName("적립금 적립 시 amount가 0보다 작을 시 예외가 발생한다.")
    void earnWithNegativeAmount() {
        //given
        Long userId = 1L;
        PointBalance pointBalance = PointBalance.create(userId);

        Long amount = -1L;
        //when
        //then
        assertThatThrownBy(() -> pointBalance.earn(amount))
                .isInstanceOf(CustomGlobalException.class)
                .hasMessage(ErrorType.POINT_BALANCE_MUST_BE_POSITIVE.getMessage());
    }

    @Test
    @DisplayName("적립금 1000원을 적립하면 PointBalance는 1000이 되어야 한다.")
    void earn(){
        //given
        Long userId = 1L;
        PointBalance pointBalance = PointBalance.create(userId);

        Long amount = 1000L;
        //when
        pointBalance.earn(amount);
        //then
        assertThat(pointBalance.getBalance()).isEqualTo(amount);
    }

    @Test
    @DisplayName("적립금 사용 시 가진 balance보다 amount가 적으면 예외가 발생한다.")
    void not_enough_point_balance_use(){
        //given
        Long userId = 1L;
        Long balance = 2000L;
        PointBalance pointBalance = PointBalance.create(userId);
        pointBalance.setBalance(balance);

        long amount = 3000L;
        //when
        //then
        assertThatThrownBy(()->pointBalance.use(amount))
                .isInstanceOf(CustomGlobalException.class)
                .hasMessage(ErrorType.NOT_ENOUGH_POINT_BALANCE.getMessage());
    }

    @Test
    @DisplayName("적립금 5000원이 있을 때 적립금 4500사용 시 500 적립금이 남아야 한다.")
    void use(){
        //given
        Long userId = 1L;
        Long balance = 5000L;
        PointBalance pointBalance = PointBalance.create(userId);
        pointBalance.setBalance(balance);

        long amount = 4500L;
        //when
        pointBalance.use(amount);
        //then
        assertThat(pointBalance.getBalance()).isEqualTo(balance - amount);
    }
}