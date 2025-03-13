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
class PointTest {

   @Test
   @DisplayName("취소된 포인트 내역에서 다시 취소를 할 경우 예외가 발생한다.")
   void already_canceled_point(){
       //given
       Long userId = 1L;
       Point point = Point.create(userId, 1000L, PointType.CANCELED, 1000L, new PointBalance());
       //when
       //then
       assertThatThrownBy(point::cancel)
               .isInstanceOf(CustomGlobalException.class)
               .hasMessage(ErrorType.ALREADY_CANCELED_POINT.getMessage());
   }

   @Test
   @DisplayName("pointBalance가 null인 경우 예외가 발생한다.")
   void null_point_balance(){
       //given
       Long userId = 1L;
       Point point = Point.create(userId, 1000L, PointType.EARNED, 1000L, null);
       //when
       //then
       assertThatThrownBy(point::cancel)
               .isInstanceOf(CustomGlobalException.class)
               .hasMessage(ErrorType.NOT_FOUND_POINT_BALANCE.getMessage());
   }

   @Test
   @DisplayName("포인트 타입이 EARNED이고 1000포인트 적립했으면 유저 적립금이 2000원 있을경우  적립금을 취소하면 1000포인트가 있어야 한다.")
   void cancel_earned(){
       //given
       Long userId = 1L;
       Point point = Point.create(userId, 1000L, PointType.EARNED, 1000L, new PointBalance(userId,2000L));
       //when
       point.cancel();
       //then
       assertThat(point.getType()).isEqualTo(PointType.CANCELED);
       assertThat(point.getPointBalance().getBalance()).isEqualTo(1000);
   }

   @Test
   @DisplayName("포인트 타입이 EARNED이고 1000포인트 적립했지만 유저 적립금이 1000원 미만일 경우 예외가 발생한다.")
   void not_enough_balance_cancel(){
       //given
       Long userId = 1L;
       Point point = Point.create(userId, 1000L, PointType.EARNED, 1000L, new PointBalance(userId,900L));
       //when
       //then
       assertThatThrownBy(point::cancel)
               .isInstanceOf(CustomGlobalException.class)
               .hasMessage(ErrorType.NOT_ENOUGH_POINT_BALANCE.getMessage());
   }

    @Test
    @DisplayName("포인트 타입이 USED이고 1000포인트 적립했으면 유저 적립금이 2000원 있을경우  적립금을 취소하면 3000포인트가 있어야 한다.")
    void cancel_used(){
        //given
        Long userId = 1L;
        Point point = Point.create(userId, 1000L, PointType.USED, 1000L, new PointBalance(userId,2000L));
        //when
        point.cancel();
        //then
        assertThat(point.getType()).isEqualTo(PointType.CANCELED);
        assertThat(point.getPointBalance().getBalance()).isEqualTo(3000);
    }

    @Test
    @DisplayName("포인트 타입이 USED, EARNED도 아니면 적립 취소 시 예외가 발생한다")
    void cancel_invalid_type(){
        //given
        Long userId = 1L;
        Point point = Point.create(userId, 1000L, null, 1000L, new PointBalance(userId,2000L));
        //when
        //then
        assertThatThrownBy(point::cancel)
                .isInstanceOf(CustomGlobalException.class)
                .hasMessage(ErrorType.INVALID_POINT_TYPE.getMessage());
    }
}