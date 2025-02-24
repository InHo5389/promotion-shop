package productservice.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorType {
    NOT_FOUND_CATEGORY(400,"카테고리가 존재하지 않습니다.")
    ;

    private final int status;
    private final String message;
}
