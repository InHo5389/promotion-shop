package orderservice.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SagaStatus {
    STARTED("주문 시작"),
    ORDER_CREATED("주문 생성 완료"),
    STOCK_DECREASED("재고 차감 완료"),
    COUPON_USED("쿠폰 사용 완료"),
    POINT_USED("포인트 사용 완료"),
    COMPLETED("주문 완료"),

    // 실패 상태
    STOCK_DECREASE_FAILED("재고 차감 실패"),
    COUPON_USE_FAILED("쿠폰 사용 실패"),
    POINT_USE_FAILED("포인트 사용 실패"),

    // 보상 상태
    COMPENSATING("보상 처리 중"),
    COMPENSATION_COMPLETED("보상 완료"),
    FAILED("주문 실패");

    private final String description;
}
