package pointservice.service.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pointservice.common.exception.CustomGlobalException;
import pointservice.common.exception.ErrorType;
import pointservice.entity.*;
import pointservice.repository.PointJpaRepository;
import pointservice.repository.PointTransactionHistoryJpaRepository;
import pointservice.service.PointBalanceRepository;
import pointservice.service.PointRepository;
import pointservice.service.dto.PointReserveRequest;

import java.util.List;

import static pointservice.entity.PointTransactionHistory.*;

@Slf4j
@Service("pointServiceV2")
@RequiredArgsConstructor
public class PointService {

    private final PointJpaRepository pointRepository;
    private final PointBalanceRepository pointBalanceRepository;
    private final PointRedisRepository pointRedisRepository;
    private final PointTransactionHistoryJpaRepository pointTransactionHistoryJpaRepository;

    /**
     * 분산 락 획득, 캐시됭 잔액 조회 (없으면 DB 조회), 포인트 잔액 증가
     * DB 저장 및 캐시 업데이트, 포인트 이력 저장
     */
    @Transactional
    public Point earn(Long userId, Long amount) {
        log.info("포인트 적립 요청 - userId: {}, amount: {}P", userId, amount);

        Long balance = pointRedisRepository.getBalanceFormCache(userId);
        if (balance == null) {
            log.debug("캐시에서 잔액 조회 실패, DB에서 조회 - userId: {}", userId);

            balance = getBalanceDb(userId);
            pointRedisRepository.updateBalanceCache(userId, balance);

            log.debug("캐시 업데이트 완료 - userId: {}, balance: {}P", userId, balance);
        }

        PointBalance pointBalance = pointBalanceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("신규 포인트 계정 생성 - userId: {}", userId);
                    return PointBalance.create(userId);
                });
        pointBalance.earn(amount);
        PointBalance savedPointBalance = pointBalanceRepository.save(pointBalance);

        pointRedisRepository.updateBalanceCache(userId, savedPointBalance.getBalance());

        Point point = Point.create(userId, amount, PointType.EARNED, savedPointBalance.getBalance(), savedPointBalance);
        Point savedPoint = pointRepository.save(point);

        log.info("포인트 적립 완료 - userId: {}, 적립액: {}P, 이전 잔액: {}P, 현재 잔액: {}P",
                userId, amount, pointBalance.getBalance(), savedPointBalance.getBalance());
        return savedPoint;
    }

    private Long getBalanceDb(Long userId) {
        log.debug("DB에서 포인트 잔액 조회 - userId: {}", userId);

        return pointBalanceRepository.findByUserId(userId)
                .map(PointBalance::getBalance)
                .orElse(0L);
    }

    @Transactional
    public void reservePoints(PointReserveRequest request) {
        Long orderId = request.orderId();
        Long userId = request.userId();
        Long amount = request.amount();

        log.info("===== 포인트 예약 시작 ===== orderId: {}, userId: {}, amount: {}", orderId, userId, amount);

        // 멱등성 체크: 이미 예약된 주문인지 확인
        List<PointTransactionHistory> existingHistories =
                pointTransactionHistoryJpaRepository.findByOrderIdAndUserIdAndType(orderId, userId, TransactionType.RESERVE);

        if (!existingHistories.isEmpty()) {
            log.warn("이미 포인트가 예약된 주문 - orderId: {}", orderId);
            return;
        }

        PointBalance pointBalance = pointBalanceRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_POINT_BALANCE));

        pointBalance.reserve(amount);

        PointTransactionHistory history = create(
                orderId,
                userId,
                amount,
                TransactionType.RESERVE
        );
        pointTransactionHistoryJpaRepository.save(history);

        log.info("포인트 예약 완료 - orderId: {}, userId: {}, amount: {}", orderId, userId, amount);
    }

    @Transactional
    public void confirmReservation(Long orderId, Long userId) {
        log.info("===== 포인트 확정 시작 ===== orderId: {}, userId: {}", orderId, userId);

        // 멱등성 체크: 이미 확정된 주문인지 확인
        List<PointTransactionHistory> confirmHistories =
                pointTransactionHistoryJpaRepository.findByOrderIdAndUserIdAndType(orderId, userId, TransactionType.CONFIRM_RESERVE);

        if (!confirmHistories.isEmpty()) {
            log.warn("이미 포인트가 확정된 주문 - orderId: {}", orderId);
            return;
        }

        List<PointTransactionHistory> reserveHistories =
                pointTransactionHistoryJpaRepository.findByOrderIdAndUserIdAndType(orderId, userId, TransactionType.RESERVE);

        if (reserveHistories.isEmpty()) {
            log.warn("포인트 예약 히스토리 없음 - orderId: {}", orderId);
            return; // 포인트 사용하지 않은 주문
        }

        for (PointTransactionHistory reserveHistory : reserveHistories) {
            PointBalance pointBalance = pointBalanceRepository.findByUserId(userId)
                    .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_POINT_BALANCE));

            pointBalance.confirmReservation(reserveHistory.getAmount());

            PointTransactionHistory confirmHistory = create(
                    orderId,
                    userId,
                    reserveHistory.getAmount(),
                    TransactionType.CONFIRM_RESERVE
            );
            pointTransactionHistoryJpaRepository.save(confirmHistory);
        }

        log.info("포인트 확정 완료 - orderId: {}", orderId);
    }

    @Transactional
    public void cancelReservation(Long orderId, Long userId) {
        log.info("===== 포인트 예약 취소 시작 ===== orderId: {}, userId: {}", orderId, userId);

        // 멱등성 체크
        List<Point> cancelPoints = pointRepository.findByOrderIdAndTypeAndUserId(
                orderId, PointType.CANCEL_RESERVE, userId);

        if (!cancelPoints.isEmpty()) {
            log.warn("이미 포인트 예약이 취소된 주문 - orderId: {}", orderId);
            return;
        }

        // 예약 내역 조회
        List<Point> reservePoints = pointRepository.findByOrderIdAndTypeAndUserId(
                orderId, PointType.RESERVED, userId);

        if (reservePoints.isEmpty()) {
            log.warn("포인트 예약 히스토리 없음 - orderId: {}", orderId);
            return;
        }

        PointBalance pointBalance = pointBalanceRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_POINT_BALANCE));

        for (Point reservePoint : reservePoints) {
            // 예약 취소
            pointBalance.cancelReservation(reservePoint.getAmount());

            // 취소 이력 저장
            Point cancelPoint = Point.create(
                    userId,
                    reservePoint.getAmount(),
                    PointType.CANCEL_RESERVE,
                    pointBalance.getBalance(),
                    pointBalance
            );
            cancelPoint.setOrderId(orderId);
            pointRepository.save(cancelPoint);

            log.info("포인트 예약 취소 완료 - orderId: {}, userId: {}, amount: {}",
                    orderId, userId, reservePoint.getAmount());
        }

        log.info("===== 포인트 예약 취소 완료 ===== orderId: {}", orderId);
    }

    @Transactional
    public void rollbackReservation(Long orderId, Long userId) {
        log.info("===== 포인트 재예약 시작 (보상) ===== orderId: {}, userId: {}", orderId, userId);

        // 멱등성 체크
        List<Point> rollbackPoints = pointRepository.findByOrderIdAndTypeAndUserId(
                orderId, PointType.ROLLBACK_RESERVE, userId);

        if (!rollbackPoints.isEmpty()) {
            log.info("포인트 재예약 스킵 - 이미 처리됨, orderId: {}", orderId);
            return;
        }

        // 취소된 내역 조회
        List<Point> cancelPoints = pointRepository.findByOrderIdAndTypeAndUserId(
                orderId, PointType.CANCEL_RESERVE, userId);

        if (cancelPoints.isEmpty()) {
            log.info("포인트 재예약 스킵 - 취소 내역 없음, orderId: {}", orderId);
            return;
        }

        PointBalance pointBalance = pointBalanceRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_POINT_BALANCE));

        for (Point cancelPoint : cancelPoints) {
            // 재예약
            pointBalance.reserve(cancelPoint.getAmount());

            // 롤백 이력 저장
            Point rollbackPoint = Point.create(
                    userId,
                    cancelPoint.getAmount(),
                    PointType.ROLLBACK_RESERVE,
                    pointBalance.getBalance(),
                    pointBalance
            );
            rollbackPoint.setOrderId(orderId);
            pointRepository.save(rollbackPoint);

            log.info("포인트 재예약 완료 - orderId: {}, userId: {}, amount: {}",
                    orderId, userId, cancelPoint.getAmount());
        }

        log.info("===== 포인트 재예약 완료 (보상) ===== orderId: {}", orderId);
    }

    @Transactional
    public Point use(Long userId, Long amount) {
        log.info("포인트 사용 요청 - userId: {}, amount: {}P", userId, amount);

        Long balance = pointRedisRepository.getBalanceFormCache(userId);
        if (balance == null) {
            balance = getBalanceDb(userId);
            pointRedisRepository.updateBalanceCache(userId, balance);
        }

        if (balance < amount) {
            log.info("포인트 잔액 부족 - userId: {}, 요청 금액: {}P, 현재 잔액: {}P", userId, amount, balance);
            throw new CustomGlobalException(ErrorType.INSUFFICIENT_POINT_BALANCE);
        }

        if (amount % 10 == 0 && amount % 100 != 0) {
            log.info("포인트 10원 단위 사용 불가 - userId: {}, 요청 금액: {}P", userId, amount);
            throw new CustomGlobalException(ErrorType.INVALID_POINT_AMOUNT);
        }

        PointBalance pointBalance = pointBalanceRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_POINT_BALANCE));
        Long beforeBalance = pointBalance.getBalance();

        pointBalance.use(amount);

        pointRedisRepository.updateBalanceCache(userId, pointBalance.getBalance());

        Point point = Point.create(userId, amount, PointType.USED, pointBalance.getBalance(), pointBalance);

        log.info("포인트 사용 완료 - userId: {}, 사용액: {}P, 이전 잔액: {}P, 현재 잔액: {}P",
                userId, amount, pointBalance, pointBalance.getBalance());
        return pointRepository.save(point);
    }

    @Transactional
    public void usePoints(PointReserveRequest request) {
        Long orderId = request.orderId();
        Long userId = request.userId();
        Long amount = request.amount();

        log.info("포인트 사용 시작 - orderId: {}, userId: {}, amount: {}", orderId, userId, amount);
        // ⭐ 중복 체크: 이미 처리된 주문인지 확인
        List<Point> existingPoints = pointRepository.findByOrderIdAndTypeAndUserId(
                orderId, PointType.USED, userId);

        if (!existingPoints.isEmpty()) {
            log.info("포인트 사용 스킵 - 이미 처리됨, orderId: {}, userId: {}", orderId, userId);
            throw new CustomGlobalException(ErrorType.ALREADY_PROCESSED_ORDER);
        }

        PointBalance pointBalance = pointBalanceRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_POINT_BALANCE));

        // ⭐ 즉시 차감
        pointBalance.use(amount);

        // 이력 저장 (롤백을 위해)
        Point point = Point.create(
                userId,
                amount,
                PointType.USED,
                pointBalance.getBalance(),
                pointBalance
        );
        point.setOrderId(orderId); // orderId 설정 (롤백을 위해)
        pointRepository.save(point);

        log.info("포인트 사용 완료 - orderId: {}, userId: {}, amount: {}", orderId, userId, amount);
    }

    @Transactional
    public void rollbackConfirmPoints(Long orderId, Long userId) {
        List<Point> points = pointRepository.findByOrderIdAndTypeAndUserId(
                orderId, PointType.USED, userId);

        if (points.isEmpty()) {
            log.info("포인트 롤백 - 이력 없음, orderId: {}, userId: {}", orderId, userId);
            return;
        }

        PointBalance pointBalance = pointBalanceRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_POINT_BALANCE));

        for (Point point : points) {
            // ⭐ 포인트 복구
            pointBalance.refund(point.getAmount());

            // 취소 이력 저장
            Point cancelPoint = Point.create(
                    userId,
                    point.getAmount(),
                    PointType.CANCELED,
                    pointBalance.getBalance(),
                    pointBalance
            );
            cancelPoint.setOrderId(orderId);
            pointRepository.save(cancelPoint);

            log.info("포인트 롤백 완료 - orderId: {}, userId: {}, amount: {}",
                    orderId, userId, point.getAmount());
        }
    }

    @Transactional
    public void rollbackReservePoints(Long orderId, Long userId) {

        // 멱등성 체크
        List<PointTransactionHistory> rollbackHistories =
                pointTransactionHistoryJpaRepository.findByOrderIdAndType(orderId, TransactionType.ROLLBACK_RESERVE);

        if (!rollbackHistories.isEmpty()) {
            log.info("포인트 재예약 스킵 - 이미 처리됨, orderId: {}", orderId);
            return;
        }

        // 취소된 내역 조회
        List<PointTransactionHistory> cancelHistories =
                pointTransactionHistoryJpaRepository.findByOrderIdAndType(orderId, TransactionType.CANCEL_RESERVE);

        if (cancelHistories.isEmpty()) {
            log.info("포인트 재예약 스킵 - 취소 내역 없음, orderId: {}", orderId);
            return;
        }

        PointBalance pointBalance = pointBalanceRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_POINT_BALANCE));

        for (PointTransactionHistory history : cancelHistories) {
            // 재예약
            pointBalance.reserve(history.getAmount());

            // 롤백 이력 저장
            PointTransactionHistory rollbackHistory = PointTransactionHistory.create(
                    orderId,
                    userId,
                    history.getAmount(),
                    TransactionType.ROLLBACK_RESERVE
            );
            pointTransactionHistoryJpaRepository.save(rollbackHistory);

            log.info("포인트 재예약 완료 - orderId: {}, userId: {}, amount: {}",
                    orderId, userId, history.getAmount());
        }

        log.info("===== 포인트 재예약 완료 (보상) ===== orderId: {}", orderId);
    }


    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Point cancel(Point point) {
        log.info("포인트 취소 요청 - userId: {}, pointId: {}, amount: {}P, type: {}",
                point.getUserId(), point.getId(), point.getAmount(), point.getType());

        point.cancel();

        PointBalance pointBalance = point.getPointBalance();
        pointRedisRepository.updateBalanceCache(point.getUserId(), pointBalance.getBalance());

        Point savedPoint = Point.create(point.getUserId(), point.getAmount(), PointType.CANCELED, pointBalance.getBalance(), pointBalance);

        return pointRepository.save(savedPoint);
    }

    @Transactional(readOnly = true)
    public Long getBalance(Long userId) {
        log.info("포인트 잔액 조회 요청 - userId: {}", userId);

        Long balance = pointRedisRepository.getBalanceFormCache(userId);
        if (balance != null) {
            return balance;
        }

        Long balanceDb = getBalanceDb(userId);
        pointRedisRepository.updateBalanceCache(userId, balanceDb);
        log.debug("캐시 업데이트 완료 - userId: {}, balance: {}P", userId, balanceDb);

        return balanceDb;
    }

    public Page<Point> getPointHistory(Long userId, Pageable pageable) {
        log.info("포인트 이력 조회 - userId: {}, page: {}, size: {}",
                userId, pageable.getPageNumber(), pageable.getPageSize());
        return pointRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}
