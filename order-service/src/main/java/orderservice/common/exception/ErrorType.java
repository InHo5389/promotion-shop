package orderservice.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorType {

    NOT_FOUND_ORDER(400, "주문 내역이 존재하지 않습니다."),
    NOT_FOUND_CART(400, "장바구니가 비어있습니다."),
    EMPTY_CART(400, "장바구니가 비어있습니다."),
    INVALID_ORDER_STATUS(400, "유효하지 않은 주문 상태입니다."),
    NO_PERMISSION_TO_CANCEL_ORDER(400, "인증되지 않은 유저라 주문을 취소할 수 없습니다."),
    ORDER_ALREADY_CANCELED(400, "이미 취소된 주문입니다."),
    ORDER_CANNOT_BE_CANCELED(400, "배송중이거나 배송 완료된 상품은 주문을 취소 할 수 없습니다."),
    OPTION_NOT_FOUND(400, "상품 옵션을 찾을 수 없습니다."),
    CART_EMPTY(400, "장바구니가 비어있습니다."),
    PRODUCT_NOT_SELL(400, "상품이 판매중이지 않습니다."),
    NOT_ENOUGH_STOCK(400, "상품재고가 부족합니다."),
    INVALID_COUPON(400, "쿠폰이 유효하지 않습니다."),
    NOT_ENOUGH_POINT(400, "포인트 금액은 0보다 커야 합니다"),

    NOT_FOUND_DISCOUNT_TYPE(400, "할인 정책을 찾을수 없습니다."),

    NOT_FOUND_X_USER_ID_HEADER(400, "헤더가 비어있거나 NULL입니다."),
    PRODUCT_OPTION_NOT_FOUND(400, "제품 옵션을 찾을 수 없습니다"),

    ORDER_CREATE_FAILED(500, "주문 생성에 실패하였습니다."),
    ORDER_CONFIRM_FAILED(500, "주문 생성에 실패하였습니다."),
    ORDER_CANCEL_FAILED(500, "주문 취소에 실패하였습니다."),
    CART_ITEM_NOT_FOUND(400, "장바구니 아이템을 찾을 수 없습니다."),
    COUPON_NOT_AVAILABLE(400, "쿠폰이 사용가능하지 않습니다."),
    ORDER_EXPIRED(400, "주문이 만료되었습니다."),

    PRODUCT_SERVICE_UNAVAILABLE(503, "상품 서비스 장애 : 현재 일시적으로 상품 정보를 불러올 수 없습니다. 잠시 후 다시 시도해 주세요."),
    CART_SERVICE_UNAVAILABLE(503, "장바구니 서비스 장애 : 현재 일시적으로 장바구니 정보를 불러올 수 없습니다. 잠시 후 다시 시도해 주세요."),
    POINT_SERVICE_UNAVAILABLE(503, "포인트 서비스 장애 : 현재 일시적으로 포인트 정보를 불러올 수 없습니다. 잠시 후 다시 시도해 주세요."),
    COUPON_SERVICE_UNAVAILABLE(503, "쿠폰 서비스 장애 : 현재 일시적으로 쿠폰 정보를 불러올 수 없습니다. 잠시 후 다시 시도해 주세요."),

    STOCK_RESERVATION_FAILED(400, "재고 예약에 실패했습니다."),
    COUPON_RESERVATION_FAILED(400, "쿠폰 예약에 실패했습니다."),
    POINT_RESERVATION_FAILED(400, "적립금 예약에 실패했습니다."),
    UNAUTHORIZED_ORDER_ACCESS(400,"인증되지 않은 주문입니다." );

    private final int status;
    private final String message;
}
