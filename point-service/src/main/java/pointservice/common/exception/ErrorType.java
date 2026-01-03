package pointservice.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorType {

    POINT_BALANCE_MUST_BE_POSITIVE(400, "적립금은 양수여야 합니다."),
    NOT_ENOUGH_POINT_BALANCE(400, "적립금이 부족하여 적립금을 사용할 수 없습니다."),
    NOT_FOUND_POINT_BALANCE(400, "적립금을 찾을 수 없습니다."),
    NOT_FOUND_POINT(400, "적립금 사용 내역 찾을 수 없습니다."),
    POINT_BALANCE_INVALID(400, "적립금이 NULL이거나 0보다 작습니다."),
    ALREADY_CANCELED_POINT(400, "이미 취소된 적립금 내역입니다."),
    INSUFFICIENT_POINT_BALANCE(400, "적립금이 부족합니다."),
    INVALID_POINT_TYPE(400, "유효하지 않은 적립금 유형입니다."),

    NOT_FOUND_X_USER_ID_HEADER(400, "헤더가 비어있거나 NULL입니다."),
    PRODUCT_OPTION_NOT_FOUND(400, "제품 옵션을 찾을 수 없습니다"),
    INVALID_POINT_AMOUNT(400, "포인트는 10원 단위로 사용할 수 없습니다"),
    ALREADY_PROCESSED_ORDER(400, "이미 처리된 주문입니다."),
    NOT_FOUND_RESERVE_POINT(400,"예약된 포인트가 없습니다"),
    INVALID_POINT_RESERVATION(400,"예약 재고가 부족합니다. 유효하지 않은 확정입니다."),
    POINT_CONFIRMATION_NOT_FOUND(400,"확정된 포인트를 찾을 수 없습니다." );

    private final int status;
    private final String message;
}
