package orderservice.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {
    COMPLETED("주문 완료 상태"),
    PENDING("주문 접수 상태"),
    CONFIRMED("결제 완료 상태"),
    CANCELLED("주문 취소 상태"),
    FAILED("주문 실패");

    private final String description;
}
