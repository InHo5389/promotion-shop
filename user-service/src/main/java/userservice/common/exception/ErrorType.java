package userservice.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorType {

    ALREADY_EXIST_USER(400, "이미 등록된 유저입니다."),
    NOT_FOUND_USER(400, "유저를 찾을 수 없습니다."),
    NON_AUTHORIZE_USER(400, "비밀번호가 일치하지 않아 인증되지 않은 사용자입니다"),
    NOT_FOUND_X_USER_ID_HEADER(400, "헤더가 비어있거나 NULL입니다."),
    NOT_FOUND_PRODUCT_OPTION(400, "상품 옵션을 찾을수 없습니다."),
    NOT_ENOUGH_STOCK(400, "재고가 부족합니다."),
    ALREADY_IN_CART(400, "이미 장바구니에 있는 상품입니다."),
    PRODUCT_NOT_SELL(400, "제품이 판매중이지 않습니다."),

    JSON_PARSING_FAILED(500, "Json 파싱 시 오류가 발생하였습니다."),
    NOT_FOUND_CART_PRODUCT(400, "장바구니 제품을 찾지 못했습니다.");

    private final int status;
    private final String message;
}
