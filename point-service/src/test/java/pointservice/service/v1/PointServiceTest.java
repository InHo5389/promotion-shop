package pointservice.service.v1;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import pointservice.common.exception.CustomGlobalException;
import pointservice.common.exception.ErrorType;
import pointservice.entity.Point;
import pointservice.entity.PointBalance;
import pointservice.entity.PointType;
import pointservice.service.PointBalanceRepository;
import pointservice.service.PointRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @InjectMocks
    private PointService pointService;

    @Mock
    private PointRepository pointRepository;

    @Mock
    private PointBalanceRepository pointBalanceRepository;

    @Test
    @DisplayName("포인트 적립 시 사용자의 적립금 내역이 존재하지 않으면 새로 생성한다")
    void earnWithNewPointBalance() {
        // given
        Long userId = 1L;
        Long amount = 1000L;
        PointBalance pointBalance = PointBalance.create(userId);
        pointBalance.setBalance(amount);
        Point expectedPoint = Point.create(userId, amount, PointType.EARNED, amount, pointBalance);

        when(pointBalanceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(pointBalanceRepository.save(any(PointBalance.class))).thenReturn(pointBalance);
        when(pointRepository.save(any(Point.class))).thenReturn(expectedPoint);

        // when
        Point result = pointService.earn(userId, amount);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getAmount()).isEqualTo(amount);
        assertThat(result.getType()).isEqualTo(PointType.EARNED);
        assertThat(result.getPointBalance().getBalance()).isEqualTo(amount);

        verify(pointBalanceRepository).findByUserId(userId);
        verify(pointBalanceRepository).save(any(PointBalance.class));
        verify(pointRepository).save(any(Point.class));
    }

    @Test
    @DisplayName("포인트 적립 시 사용자의 적립금 내역이 존재하면 기존 적립금에 추가한다")
    void earnWithExistingPointBalance() {
        // given
        Long userId = 1L;
        Long existingBalance = 2000L;
        Long amount = 1000L;
        PointBalance pointBalance = PointBalance.create(userId);
        pointBalance.setBalance(existingBalance);

        Point expectedPoint = Point.create(userId, amount, PointType.EARNED, existingBalance + amount, pointBalance);

        when(pointBalanceRepository.findByUserId(userId)).thenReturn(Optional.of(pointBalance));
        when(pointBalanceRepository.save(any(PointBalance.class))).thenReturn(pointBalance);
        when(pointRepository.save(any(Point.class))).thenReturn(expectedPoint);

        // when
        Point result = pointService.earn(userId, amount);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getAmount()).isEqualTo(amount);
        assertThat(result.getType()).isEqualTo(PointType.EARNED);
        assertThat(result.getPointBalance().getBalance()).isEqualTo(existingBalance + amount);

        verify(pointBalanceRepository).findByUserId(userId);
        verify(pointBalanceRepository).save(pointBalance);
        verify(pointRepository).save(any(Point.class));
    }

    @Test
    @DisplayName("포인트 사용 시 사용자의 적립금 내역이 존재하지 않으면 예외가 발생한다")
    void useWithNoPointBalance() {
        // given
        Long userId = 1L;
        Long amount = 1000L;

        when(pointBalanceRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> pointService.use(userId, amount))
                .isInstanceOf(CustomGlobalException.class)
                .hasMessage(ErrorType.NOT_FOUND_POINT_BALANCE.getMessage());

        verify(pointBalanceRepository).findByUserId(userId);
        verifyNoInteractions(pointRepository);
    }

    @Test
    @DisplayName("포인트 사용 시 적립금이 충분하면 사용 내역을 저장한다")
    void useWithSufficientBalance() {
        // given
        Long userId = 1L;
        Long existingBalance = 2000L;
        Long amount = 1000L;

        PointBalance pointBalance = PointBalance.create(userId);
        pointBalance.setBalance(existingBalance);

        Point expectedPoint = Point.create(userId, amount, PointType.USED, existingBalance - amount, pointBalance);

        when(pointBalanceRepository.findByUserId(userId)).thenReturn(Optional.of(pointBalance));
        when(pointRepository.save(any(Point.class))).thenReturn(expectedPoint);

        // when
        Point result = pointService.use(userId, amount);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getAmount()).isEqualTo(amount);
        assertThat(result.getType()).isEqualTo(PointType.USED);
        assertThat(result.getBalanceSnapshot()).isEqualTo(existingBalance - amount);

        verify(pointBalanceRepository).findByUserId(userId);
        verify(pointRepository).save(any(Point.class));
    }

    @Test
    @DisplayName("포인트 취소 시 존재하지 않는 포인트 ID를 요청하면 예외가 발생한다")
    void cancelWithNonExistentPoint() {
        // given
        Long pointId = 999L;

        when(pointRepository.findById(pointId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> pointService.cancel(pointId))
                .isInstanceOf(CustomGlobalException.class)
                .hasMessage(ErrorType.NOT_FOUND_POINT.getMessage());

        verify(pointRepository).findById(pointId);
    }

    @Test
    @DisplayName("포인트 취소가 성공하면 취소된 포인트를 반환한다")
    void cancelSuccessfully() {
        // given
        Long pointId = 1L;
        Long userId = 1L;
        Long amount = 1000L;

        PointBalance pointBalance = new PointBalance(userId, 2000L);
        Point point = Point.create(userId, amount, PointType.EARNED, 2000L, pointBalance);

        when(pointRepository.findById(pointId)).thenReturn(Optional.of(point));

        // when
        Point result = pointService.cancel(pointId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getType()).isEqualTo(PointType.CANCELED);
        assertThat(result.getPointBalance().getBalance()).isEqualTo(1000L);

        verify(pointRepository).findById(pointId);
    }

    @Test
    @DisplayName("잔액 조회 시 사용자의 적립금 내역이 존재하지 않으면 0을 반환한다")
    void getBalanceWithNoPointBalance() {
        // given
        Long userId = 1L;

        when(pointBalanceRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // when
        Long result = pointService.getBalance(userId);

        // then
        assertThat(result).isEqualTo(0L);

        verify(pointBalanceRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("잔액 조회 시 사용자의 적립금 내역이 존재하면 현재 잔액을 반환한다")
    void getBalanceWithExistingPointBalance() {
        // given
        Long userId = 1L;
        Long expectedBalance = 2000L;

        PointBalance pointBalance = new PointBalance(userId, expectedBalance);

        when(pointBalanceRepository.findByUserId(userId)).thenReturn(Optional.of(pointBalance));

        // when
        Long result = pointService.getBalance(userId);

        // then
        assertThat(result).isEqualTo(expectedBalance);

        verify(pointBalanceRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("포인트 내역 조회 시 사용자의 포인트 내역을 시간 역순으로 반환한다")
    void getPointHistoryReturnsOrderedPagedResults() {
        // given
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        Point point1 = Point.create(userId, 1000L, PointType.EARNED, 1000L, null);
        Point point2 = Point.create(userId, 500L, PointType.USED, 500L, null);
        List<Point> points = List.of(point1, point2);

        Page<Point> expectedPage = new PageImpl<>(points, pageable, points.size());

        when(pointRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)).thenReturn(expectedPage);

        // when
        Page<Point> result = pointService.getPointHistory(userId, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);

        verify(pointRepository).findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}