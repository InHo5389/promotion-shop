package orderservice.common.handler;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.common.exception.CompensationException;
import orderservice.common.exception.CustomGlobalException;
import orderservice.common.exception.ErrorType;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class CustomExceptionHandler {

    @ExceptionHandler(CustomGlobalException.class)
    public ResponseEntity<?> apiExceptionHandler(CustomGlobalException e) {
        ErrorType errorType = e.getErrorType();
        HttpStatus httpStatus = HttpStatus.valueOf(errorType.getStatus());

        log.info(e.getMessage());

        return ResponseEntity
                .status(httpStatus.value())
                .body(new ExceptionResponse(httpStatus.value(), httpStatus, e.getMessage()));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public ResponseEntity<?> bindException(BindException e) {
        HttpStatus badRequest = HttpStatus.BAD_REQUEST;

        String errorMessage = e.getBindingResult()
                .getFieldError()
                .getDefaultMessage();

        log.info(errorMessage);

        return ResponseEntity
                .status(badRequest)
                .body(new ExceptionResponse(badRequest.value(), badRequest, errorMessage));
    }

    @ExceptionHandler(CompensationException.class)
    public ResponseEntity<ExceptionResponse> handleFeignException(CompensationException e) {
        int status = HttpStatus.INTERNAL_SERVER_ERROR.value();

        return ResponseEntity.status(status)
                .body(ExceptionResponse.builder()
                        .code(status)
                        .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .message("외부 서비스 에러: " + extractCleanMessage(e.getMessage()))
                        .build());
    }

    private String extractCleanMessage(String rawMessage) {
        String extracted = StringUtils.substringBetween(rawMessage, "\"message\":\"", "\"");
        return extracted != null ? extracted : "요청 처리 중 오류가 발생했습니다";
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ExceptionResponse {
        private int code;
        private HttpStatus httpStatus;
        private String message;
    }
}
