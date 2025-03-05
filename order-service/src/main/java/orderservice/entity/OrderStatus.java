package orderservice.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {
    PENDING("주문 접수 상태"),
    PAID("결제 완료 상태"),
    SHIPPED("배송중 상태"),
    DELIVERED("배송 완료 상태"),
    CANCELLED("주문 취소 상태"),
    REFUNDED("환불 완료 상태");

    private final String description;
}
