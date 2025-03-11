package orderservice.cleint.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import orderservice.common.exception.CustomGlobalException;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Component
public class FeignErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        HttpStatus status = HttpStatus.valueOf(response.status());

        try {
            if (response.body() != null) {
                String errorBody = IOUtils.toString(response.body().asInputStream(), "UTF-8");

                try {
                    JsonNode root = objectMapper.readTree(errorBody);
                    if (root.has("message")) {
                        String errorMessage = root.get("message").asText();
                        return new CustomErrorException(status, errorMessage);
                    }
                } catch (JsonProcessingException e) {
                    log.debug("Failed to parse error response as JSON", e);
                }

                return new CustomErrorException(status, errorBody);
            }
        } catch (Exception e) {
            log.error("Error reading response body", e);
        }

        return new CustomErrorException(status, response.reason());
    }

    public static class CustomErrorException extends RuntimeException {
        private final HttpStatus status;
        private final String message;

        public CustomErrorException(HttpStatus status, String message) {
            super(message);
            this.status = status;
            this.message = message;
        }

        public HttpStatus getStatus() {
            return status;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }
}
