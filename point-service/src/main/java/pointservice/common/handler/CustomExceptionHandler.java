package pointservice.common.handler;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pointservice.common.exception.CustomGlobalException;
import pointservice.common.exception.ErrorType;

@RestControllerAdvice
public class CustomExceptionHandler {

    @ExceptionHandler(CustomGlobalException.class)
    public ResponseEntity<?> apiExceptionHandler(CustomGlobalException e) {
        ErrorType errorType = e.getErrorType();
        HttpStatus httpStatus = HttpStatus.valueOf(errorType.getStatus());

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

        return ResponseEntity
                .status(badRequest)
                .body(new ExceptionResponse(badRequest.value(), badRequest, errorMessage));
    }

    @Getter
    @AllArgsConstructor
    private static class ExceptionResponse {
        private int code;
        private HttpStatus httpStatus;
        private String message;
    }
}
