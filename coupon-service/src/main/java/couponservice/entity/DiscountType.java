package couponservice.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DiscountType {

    FIXED_DISCOUNT("정책 할인"),
    RATE_DISCOUNT("정률 할인")
    ;

    private final String description;
}
