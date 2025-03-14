package pointservice.service.v2;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointServiceV2Test {

    @InjectMocks
    private PointService pointService;

    @Mock
    private PointRepository pointRepository;

    @Mock
    private PointBalanceRepository pointBalanceRepository;

    @Mock
    private PointRedisRepository pointRedisRepository;

    @Test
    @DisplayName("포인트 적립 시 Redis 캐시에 잔액이 있으면 DB 조회 없이 캐시된 잔액을 사용한다")
    void earnWithCachedBalance() {
        // given
        Long userId = 1L;
        Long cachedBalance = 2000L;
        Long amount = 1000L;

        PointBalance pointBalance = PointBalance.create(userId);
        pointBalance.setBalance(cachedBalance);

        PointBalance updatedPointBalance = PointBalance.create(userId);
        updatedPointBalance.setBalance(cachedBalance + amount);

        Point expectedPoint = Point.create(userId, amount, PointType.EARNED, cachedBalance + amount, updatedPointBalance);

        when(pointRedisRepository.getBalanceFormCache(userId)).thenReturn(cachedBalance);
        when(pointBalanceRepository.findByUserId(userId)).thenReturn(Optional.of(pointBalance));
        when(pointBalanceRepository.save(any(PointBalance.class))).thenReturn(updatedPointBalance);
        when(pointRepository.save(any(Point.class))).thenReturn(expectedPoint);

        // when
        Point result = pointService.earn(userId, amount);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getAmount()).isEqualTo(amount);
        assertThat(result.getType()).isEqualTo(PointType.EARNED);
        assertThat(result.getBalanceSnapshot()).isEqualTo(cachedBalance + amount);

        verify(pointRedisRepository).getBalanceFormCache(userId);
        verify(pointBalanceRepository).findByUserId(userId);
        verify(pointBalanceRepository).save(any(PointBalance.class));
        verify(pointRedisRepository).updateBalanceCache(userId, cachedBalance + amount);
        verify(pointRepository).save(any(Point.class));
        // Redis에 없는 경우에만 DB 조회하므로, getBalanceDb 메서드는 호출되지 않아야 함
        verify(pointBalanceRepository, never()).findByUserId(eq(userId));
    }

    @Test
    @DisplayName("포인트 적립 시 Redis 캐시에 잔액이 없으면 DB에서 조회하여 캐시에 저장한다")
    void earnWithNoCachedBalance() {
        // given
        Long userId = 1L;
        Long dbBalance = 2000L;
        Long amount = 1000L;

        PointBalance pointBalance = PointBalance.create(userId);
        pointBalance.setBalance(dbBalance);

        PointBalance updatedPointBalance = PointBalance.create(userId);
        updatedPointBalance.setBalance(dbBalance + amount);

        Point expectedPoint = Point.create(userId, amount, PointType.EARNED, dbBalance + amount, updatedPointBalance);

        when(pointRedisRepository.getBalanceFormCache(userId)).thenReturn(null);
        when(pointBalanceRepository.findByUserId(userId)).thenReturn(Optional.of(pointBalance));
        when(pointBalanceRepository.save(any(PointBalance.class))).thenReturn(updatedPointBalance);
        when(pointRepository.save(any(Point.class))).thenReturn(expectedPoint);

        // when
        Point result = pointService.earn(userId, amount);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getAmount()).isEqualTo(amount);
        assertThat(result.getType()).isEqualTo(PointType.EARNED);
        assertThat(result.getBalanceSnapshot()).isEqualTo(dbBalance + amount);

        verify(pointRedisRepository).getBalanceFormCache(userId);
        verify(pointBalanceRepository).findByUserId(userId);
        verify(pointBalanceRepository).save(any(PointBalance.class));
        // DB에서 조회한 후 Redis에 저장하는지 확인
        verify(pointRedisRepository).updateBalanceCache(eq(userId), eq(dbBalance));
        // 포인트 적립 후 업데이트된 잔액을 Redis에 저장하는지 확인
        verify(pointRedisRepository).updateBalanceCache(eq(userId), eq(dbBalance + amount));
        verify(pointRepository).save(any(Point.class));
    }

    @Test
    @DisplayName("포인트 적립 시 사용자의 적립금 내역이 존재하지 않으면 새로 생성한다")
    void earnWithNewPointBalance() {
        // given
        Long userId = 1L;
        Long amount = 1000L;

        PointBalance newPointBalance = PointBalance.create(userId);
        newPointBalance.setBalance(amount);

        Point expectedPoint = Point.create(userId, amount, PointType.EARNED, amount, newPointBalance);

        when(pointRedisRepository.getBalanceFormCache(userId)).thenReturn(null);
        when(pointBalanceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(pointBalanceRepository.save(any(PointBalance.class))).thenReturn(newPointBalance);
        when(pointRepository.save(any(Point.class))).thenReturn(expectedPoint);

        // when
        Point result = pointService.earn(userId, amount);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getAmount()).isEqualTo(amount);
        assertThat(result.getType()).isEqualTo(PointType.EARNED);
        assertThat(result.getBalanceSnapshot()).isEqualTo(amount);

        verify(pointRedisRepository).getBalanceFormCache(userId);
        verify(pointBalanceRepository).findByUserId(userId);
        verify(pointBalanceRepository).save(any(PointBalance.class));
        // 처음에는 DB 조회 결과 0을 Redis에 저장
        verify(pointRedisRepository).updateBalanceCache(eq(userId), eq(0L));
        // 포인트 적립 후 업데이트된 잔액을 Redis에 저장
        verify(pointRedisRepository).updateBalanceCache(eq(userId), eq(amount));
        verify(pointRepository).save(any(Point.class));
    }

    @Test
    @DisplayName("포인트 사용 시 Redis 캐시에 잔액이 충분하면 포인트를 사용한다")
    void useWithSufficientCachedBalance() {
        // given
        Long userId = 1L;
        Long cachedBalance = 2000L;
        Long amount = 1000L;

        PointBalance pointBalance = PointBalance.create(userId);
        pointBalance.setBalance(cachedBalance);

        Point expectedPoint = Point.create(userId, amount, PointType.USED, cachedBalance - amount, pointBalance);

        when(pointRedisRepository.getBalanceFormCache(userId)).thenReturn(cachedBalance);
        when(pointBalanceRepository.findByUserId(userId)).thenReturn(Optional.of(pointBalance));
        when(pointRepository.save(any(Point.class))).thenReturn(expectedPoint);

        // when
        Point result = pointService.use(userId, amount);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getAmount()).isEqualTo(amount);
        assertThat(result.getType()).isEqualTo(PointType.USED);
        assertThat(result.getBalanceSnapshot()).isEqualTo(cachedBalance - amount);

        verify(pointRedisRepository).getBalanceFormCache(userId);
        verify(pointBalanceRepository).findByUserId(userId);
        // 포인트 사용 후 업데이트된 잔액을 Redis에 저장하는지 확인
        verify(pointRedisRepository).updateBalanceCache(eq(userId), eq(cachedBalance - amount));
        verify(pointRepository).save(any(Point.class));
    }

    @Test
    @DisplayName("포인트 사용 시 잔액이 부족하면 예외가 발생한다")
    void useWithInsufficientBalance() {
        // given
        Long userId = 1L;
        Long cachedBalance = 500L;
        Long amount = 1000L;

        when(pointRedisRepository.getBalanceFormCache(userId)).thenReturn(cachedBalance);

        // when & then
        assertThatThrownBy(() -> pointService.use(userId, amount))
                .isInstanceOf(CustomGlobalException.class)
                .hasMessage(ErrorType.INSUFFICIENT_POINT_BALANCE.getMessage());

        verify(pointRedisRepository).getBalanceFormCache(userId);
        verifyNoMoreInteractions(pointBalanceRepository, pointRepository, pointRedisRepository);
    }

    @Test
    @DisplayName("포인트 사용 시 Redis 캐시에 잔액이 없으면 DB에서 조회하여 캐시에 저장한다")
    void useWithNoCachedBalance() {
        // given
        Long userId = 1L;
        Long dbBalance = 2000L;
        Long amount = 1000L;

        PointBalance pointBalance = PointBalance.create(userId);
        pointBalance.setBalance(dbBalance);

        Point expectedPoint = Point.create(userId, amount, PointType.USED, dbBalance - amount, pointBalance);

        when(pointRedisRepository.getBalanceFormCache(userId)).thenReturn(null);
        when(pointBalanceRepository.findByUserId(userId)).thenReturn(Optional.of(pointBalance));
        when(pointRepository.save(any(Point.class))).thenReturn(expectedPoint);

        // when
        Point result = pointService.use(userId, amount);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getAmount()).isEqualTo(amount);
        assertThat(result.getType()).isEqualTo(PointType.USED);
        assertThat(result.getBalanceSnapshot()).isEqualTo(dbBalance - amount);

        verify(pointRedisRepository).getBalanceFormCache(userId);
        verify(pointBalanceRepository).findByUserId(userId);
        // DB에서 조회한 후 Redis에 저장하는지 확인
        verify(pointRedisRepository).updateBalanceCache(eq(userId), eq(dbBalance));
        // 포인트 사용 후 업데이트된 잔액을 Redis에 저장하는지 확인
        verify(pointRedisRepository).updateBalanceCache(eq(userId), eq(dbBalance - amount));
        verify(pointRepository).save(any(Point.class));
    }

    @Test
    @DisplayName("포인트 취소 시 취소된 포인트를 저장하고 Redis 캐시를 업데이트한다")
    void cancelSuccessfully() {
        // given
        Long userId = 1L;
        Long pointId = 1L;
        Long amount = 1000L;
        Long initialBalance = 1000L;

        PointBalance pointBalance = new PointBalance(userId, initialBalance);
        Point point = Point.create(userId, amount, PointType.USED, initialBalance, pointBalance);

        // cancel 호출 시 포인트 밸런스가 업데이트됨 (USED일 경우 다시 복구)
        Point canceledPoint = Point.create(userId, amount, PointType.CANCELED, initialBalance + amount, pointBalance);

        when(pointRepository.save(any(Point.class))).thenReturn(canceledPoint);

        // when
        Point result = pointService.cancel(point);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getAmount()).isEqualTo(amount);
        assertThat(result.getType()).isEqualTo(PointType.CANCELED);

        verify(pointRepository).save(any(Point.class));
        verify(pointRedisRepository).updateBalanceCache(eq(userId), eq(initialBalance + amount));
    }

    @Test
    @DisplayName("잔액 조회 시 Redis 캐시에 잔액이 있으면 캐시된 잔액을 반환한다")
    void getBalanceWithCachedBalance() {
        // given
        Long userId = 1L;
        Long cachedBalance = 2000L;

        when(pointRedisRepository.getBalanceFormCache(userId)).thenReturn(cachedBalance);

        // when
        Long result = pointService.getBalance(userId);

        // then
        assertThat(result).isEqualTo(cachedBalance);

        verify(pointRedisRepository).getBalanceFormCache(userId);
        verifyNoMoreInteractions(pointBalanceRepository);
        verifyNoMoreInteractions(pointRedisRepository);
    }

    @Test
    @DisplayName("잔액 조회 시 Redis 캐시에 잔액이 없으면 DB에서 조회하여 캐시에 저장한다")
    void getBalanceWithNoCachedBalance() {
        // given
        Long userId = 1L;
        Long dbBalance = 2000L;

        when(pointRedisRepository.getBalanceFormCache(userId)).thenReturn(null);
        when(pointBalanceRepository.findByUserId(userId)).thenReturn(Optional.of(new PointBalance(userId, dbBalance)));

        // when
        Long result = pointService.getBalance(userId);

        // then
        assertThat(result).isEqualTo(dbBalance);

        verify(pointRedisRepository).getBalanceFormCache(userId);
        verify(pointBalanceRepository).findByUserId(userId);
        verify(pointRedisRepository).updateBalanceCache(userId, dbBalance);
    }

    @Test
    @DisplayName("잔액 조회 시 Redis 캐시와 DB 모두에 사용자 정보가 없으면 0을 반환한다")
    void getBalanceWithNoUserData() {
        // given
        Long userId = 1L;

        when(pointRedisRepository.getBalanceFormCache(userId)).thenReturn(null);
        when(pointBalanceRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // when
        Long result = pointService.getBalance(userId);

        // then
        assertThat(result).isEqualTo(0L);

        verify(pointRedisRepository).getBalanceFormCache(userId);
        verify(pointBalanceRepository).findByUserId(userId);
        verify(pointRedisRepository).updateBalanceCache(userId, 0L);
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