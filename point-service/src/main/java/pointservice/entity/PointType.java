package pointservice.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PointType {

    USED("사용"),
    EARNED("적립"),
    CANCELED("취소"),
    CANCEL_RESERVE("예약 취소"),
    RESERVED("예약"),
    ROLLBACK_RESERVE("예약 적립금 복구");

    private final String description;
}
