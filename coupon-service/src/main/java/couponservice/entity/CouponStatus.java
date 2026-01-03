package couponservice.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CouponStatus {

    AVAILABLE("사용 가능"),
    RESERVED("예약됨"),
    USED("사용됨"),
    EXPIRED("만료됨"),
    CANCELED("취소됨")
    ;

    private final String description;
}
