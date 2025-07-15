package orderservice.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorType {

    NOT_FOUND_ORDER(400, "주문 내역이 존재하지 않습니다."),
    NO_PERMISSION_TO_CANCEL_ORDER(400, "인증되지 않은 유저라 주문을 취소할 수 없습니다."),
    ORDER_ALREADY_CANCELED(400, "이미 취소된 주문입니다."),
    ORDER_CANNOT_BE_CANCELED(400, "배송중이거나 배송 완료된 상품은 주문을 취소 할 수 없습니다."),
    OPTION_NOT_FOUND(400, "상품 옵션을 찾을 수 없습니다."),
    CART_EMPTY(400, "장바구니가 비어있습니다."),
    PRODUCT_NOT_SELL(400, "상품이 판매중이지 않습니다."),
    NOT_ENOUGH_STOCK(400, "상품재고가 부족합니다."),
    INVALID_COUPON(400, "쿠폰이 유효하지 않습니다."),

    NOT_FOUND_X_USER_ID_HEADER(400, "헤더가 비어있거나 NULL입니다."),
    PRODUCT_OPTION_NOT_FOUND(400, "제품 옵션을 찾을 수 없습니다"),
    PRODUCT_SERVICE_UNAVAILABLE(200, "현재 일시적으로 상품 정보를 불러올 수 없습니다. 잠시 후 다시 시도해 주세요."),
    CART_SERVICE_UNAVAILABLE(200, "현재 일시적으로 장바구니 정보를 불러올 수 없습니다. 잠시 후 다시 시도해 주세요."),
    POINT_SERVICE_UNAVAILABLE(200, "현재 일시적으로 포인트 정보를 불러올 수 없습니다. 잠시 후 다시 시도해 주세요."),
    COUPON_SERVICE_UNAVAILABLE(200, "현재 일시적으로 쿠폰 정보를 불러올 수 없습니다. 잠시 후 다시 시도해 주세요.");

    private final int status;
    private final String message;
}
