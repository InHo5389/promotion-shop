package orderservice.common.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.common.exception.CustomGlobalException;
import orderservice.common.exception.ErrorType;
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
    public ResponseEntity<?> bindException(BindException e){
        HttpStatus badRequest = HttpStatus.BAD_REQUEST;

        String errorMessage = e.getBindingResult()
                .getFieldError()
                .getDefaultMessage();

        log.info(errorMessage);

        return ResponseEntity
                .status(badRequest)
                .body(new ExceptionResponse(badRequest.value(), badRequest, errorMessage));
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ExceptionResponse> handleFeignException(FeignException e) {
        log.error("Feign exception: {}", e.getMessage());

        // FeignException의 status code 사용
        int status = e.status();

        return ResponseEntity.status(status)
                .body(ExceptionResponse.builder()
                        .code(status)
                        .message("외부 서비스 에러: " + extractErrorMessage(e))
                        .build());
    }

    private String extractErrorMessage(FeignException e) {
        try {
            String content = e.contentUTF8();

            log.info("=== Feign 응답 디버깅 ===");
            log.info("Status: {}", e.status());
            log.info("Content: {}", content);
            log.info("========================");

            if (content == null || content.trim().isEmpty()) {
                return "응답 내용이 없습니다";
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(content);

            return node.has("message") ? node.get("message").asText() : "요청 처리 실패";

        } catch (Exception ex) {
            log.error("JSON 파싱 실패", ex);
            return "서비스 호출 실패";
        }
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
