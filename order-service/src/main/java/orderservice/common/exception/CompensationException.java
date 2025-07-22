package orderservice.common.exception;

import lombok.Getter;

@Getter
public class CompensationException extends RuntimeException{

    private String message;

    public CompensationException(String message) {
        super(message);
        this.message = message;
    }
}
