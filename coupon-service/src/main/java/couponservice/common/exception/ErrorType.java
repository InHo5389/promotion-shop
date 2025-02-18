package couponservice.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorType {

    COUPON_ALREADY_USED(400, "이미 사용된 쿠폰입니다."),
    COUPON_NOT_USED(400, "사용된 쿠폰이 아니라 취소할 수 없습니다.."),
    COUPON_EXPIRED(400, "쿠폰이 만료되었습니다."),
    COUPON_NON_USED(400, "사용되지 않은 쿠폰입니다."),
    NOT_FOUND_COUPON_POLICY(400, "쿠폰 정책이 없습니다."),
    NOT_FOUND_COUPON(400, "쿠폰을 찾을 수 없습니다."),
    COUPON_NOT_ISSUABLE_PERIOD(400, "현재 쿠폰을 발행할 수 있는 기간이 아닙니다."),
    COUPON_QUANTITY_EXHAUSTED(400, "쿠폰이 모두 소진되었습니다."),

    NOT_FOUND_X_USER_ID_HEADER(400,"헤더가 비어있거나 NULL입니다.")
    ;

    private final int status;
    private final String message;
}
