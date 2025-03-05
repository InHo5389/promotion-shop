package productservice.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorType {
    NOT_FOUND_CATEGORY(400,"카테고리가 존재하지 않습니다."),
    NOT_FOUND_PRODUCT(400,"상품이 존재하지 않습니다."),
    NOT_FOUND_PRODUCT_OPTION(400,"상품 옵션이 존재하지 않습니다."),
    NOT_ENOUGH_STOCK(400, "재고가 부족합니다."),
    LOCK_ACQUISITION_FAILED(400, "LOCK_ACQUISITION_FAILED."),

    ;

    private final int status;
    private final String message;
}
