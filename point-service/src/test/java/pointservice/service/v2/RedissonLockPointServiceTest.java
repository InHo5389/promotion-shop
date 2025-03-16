package pointservice.service.v2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import pointservice.common.exception.CustomGlobalException;
import pointservice.common.exception.ErrorType;
import pointservice.entity.Point;
import pointservice.entity.PointBalance;
import pointservice.entity.PointType;
import pointservice.service.PointRepository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedissonLockPointServiceTest {

    @InjectMocks
    private RedissonLockPointService redissonLockPointService;

    @Mock
    private PointService pointService;

    @Mock
    private PointRepository pointRepository;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @Test
    @DisplayName("포인트 적립 시 Redisson 락을 획득하고 PointService.earn을 호출한다")
    void earnWithLockAcquisition() throws InterruptedException {
        // given
        Long userId = 1L;
        Long amount = 1000L;

        Point expectedPoint = Point.create(userId, amount, PointType.EARNED, amount, new PointBalance(userId, amount));

        when(redissonClient.getLock("point:lock:" + userId)).thenReturn(rLock);
        when(rLock.tryLock(3L, 3L, TimeUnit.SECONDS)).thenReturn(true);
        when(pointService.earn(userId, amount)).thenReturn(expectedPoint);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        // when
        Point result = redissonLockPointService.earn(userId, amount);

        // then
        assertThat(result).isEqualTo(expectedPoint);

        verify(redissonClient).getLock("point:lock:" + userId);
        verify(rLock).tryLock(3L, 3L, TimeUnit.SECONDS);
        verify(pointService).earn(userId, amount);
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("포인트 적립 시 락 획득에 실패하면 예외가 발생한다")
    void earnWithLockAcquisitionFailure() throws InterruptedException {
        // given
        Long userId = 1L;
        Long amount = 1000L;

        when(redissonClient.getLock("point:lock:" + userId)).thenReturn(rLock);
        when(rLock.tryLock(3L, 3L, TimeUnit.SECONDS)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> redissonLockPointService.earn(userId, amount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to acquire lock for user: " + userId);

        verify(redissonClient).getLock("point:lock:" + userId);
        verify(rLock).tryLock(3L, 3L, TimeUnit.SECONDS);
        verifyNoInteractions(pointService);
        verify(rLock, never()).unlock();
    }

    @Test
    @DisplayName("포인트 적립 시 락 획득 중 인터럽트가 발생하면 예외가 발생한다")
    void earnWithInterruptedException() throws InterruptedException {
        // given
        Long userId = 1L;
        Long amount = 1000L;

        when(redissonClient.getLock("point:lock:" + userId)).thenReturn(rLock);
        when(rLock.tryLock(3L, 3L, TimeUnit.SECONDS)).thenThrow(new InterruptedException("Lock interrupted"));

        // when & then
        assertThatThrownBy(() -> redissonLockPointService.earn(userId, amount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LOCK acquisition was interrupted");

        verify(redissonClient).getLock("point:lock:" + userId);
        verify(rLock).tryLock(3L, 3L, TimeUnit.SECONDS);
        verifyNoInteractions(pointService);
    }

    @Test
    @DisplayName("포인트 사용 시 Redisson 락을 획득하고 PointService.use를 호출한다")
    void useWithLockAcquisition() throws InterruptedException {
        // given
        Long userId = 1L;
        Long amount = 1000L;
        Long balance = 2000L;

        Point expectedPoint = Point.create(userId, amount, PointType.USED, balance - amount, new PointBalance(userId, balance - amount));

        when(redissonClient.getLock("point:lock:" + userId)).thenReturn(rLock);
        when(rLock.tryLock(3L, 3L, TimeUnit.SECONDS)).thenReturn(true);
        when(pointService.use(userId, amount)).thenReturn(expectedPoint);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        // when
        Point result = redissonLockPointService.use(userId, amount);

        // then
        assertThat(result).isEqualTo(expectedPoint);

        verify(redissonClient).getLock("point:lock:" + userId);
        verify(rLock).tryLock(3L, 3L, TimeUnit.SECONDS);
        verify(pointService).use(userId, amount);
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("포인트 사용 시 락 획득에 실패하면 예외가 발생한다")
    void useWithLockAcquisitionFailure() throws InterruptedException {
        // given
        Long userId = 1L;
        Long amount = 1000L;

        when(redissonClient.getLock("point:lock:" + userId)).thenReturn(rLock);
        when(rLock.tryLock(3L, 3L, TimeUnit.SECONDS)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> redissonLockPointService.use(userId, amount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to acquire lock for user: " + userId);

        verify(redissonClient).getLock("point:lock:" + userId);
        verify(rLock).tryLock(3L, 3L, TimeUnit.SECONDS);
        verifyNoInteractions(pointService);
        verify(rLock, never()).unlock();
    }

    @Test
    @DisplayName("포인트 취소 시 포인트 ID로 포인트를 찾고 Redisson 락을 획득한 후 PointService.cancel을 호출한다")
    void cancelWithLockAcquisition() throws InterruptedException {
        // given
        Long pointId = 1L;
        Long userId = 1L;
        Long amount = 1000L;
        Long balance = 2000L;

        PointBalance pointBalance = new PointBalance(userId, balance);
        Point point = Point.create(userId, amount, PointType.EARNED, balance, pointBalance);
        Point canceledPoint = Point.create(userId, amount, PointType.CANCELED, balance - amount, pointBalance);

        when(pointRepository.findById(pointId)).thenReturn(Optional.of(point));
        when(redissonClient.getLock("point:lock:" + userId)).thenReturn(rLock);
        when(rLock.tryLock(3L, 3L, TimeUnit.SECONDS)).thenReturn(true);
        when(pointService.cancel(point)).thenReturn(canceledPoint);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        // when
        Point result = redissonLockPointService.cancel(pointId);

        // then
        assertThat(result).isEqualTo(canceledPoint);

        verify(pointRepository).findById(pointId);
        verify(redissonClient).getLock("point:lock:" + userId);
        verify(rLock).tryLock(3L, 3L, TimeUnit.SECONDS);
        verify(pointService).cancel(point);
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("포인트 취소 시 존재하지 않는 포인트 ID를 요청하면 예외가 발생한다")
    void cancelWithNonExistentPoint() {
        // given
        Long pointId = 999L;

        when(pointRepository.findById(pointId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> redissonLockPointService.cancel(pointId))
                .isInstanceOf(CustomGlobalException.class)
                .hasMessage(ErrorType.NOT_FOUND_POINT.getMessage());

        verify(pointRepository).findById(pointId);
        verifyNoInteractions(redissonClient, pointService);
    }
}