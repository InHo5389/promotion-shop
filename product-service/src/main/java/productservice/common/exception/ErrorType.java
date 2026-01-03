package productservice.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorType {
    NOT_FOUND_CATEGORY(400,"카테고리가 존재하지 않습니다."),
    NOT_FOUND_PRODUCT(400,"상품이 존재하지 않습니다."),
    NOT_FOUND_PRODUCT_OPTION(400,"상품 옵션이 존재하지 않습니다."),
    NOT_FOUND_STOCK(400,"재고가 존재하지 않습니다."),
    NOT_ENOUGH_STOCK(400, "재고가 부족합니다."),
    LOCK_ACQUISITION_FAILED(400, "LOCK_ACQUISITION_FAILED."),
    ALREADY_PROCESSED_ORDER(400, "이미 처리된 주문입니다."),
    NOT_FOUND_RESERVE_STOCK(400, "예약 이력이 존재하지 않습니다."),
    INVALID_STOCK_RESERVATION(400, "유효하지 않은 재고 예약입니다."),
    STOCK_CONFIRMATION_NOT_FOUND(400, "재고 확정 이력이 없습니다.");

    private final int status;
    private final String message;
}
