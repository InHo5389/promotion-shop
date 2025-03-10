package apigateway.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class ErrorResponseUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Mono<Void> createUnauthorizedResponse(ServerWebExchange exchange, ErrorType errorType) {
        return createErrorResponse(exchange, HttpStatus.UNAUTHORIZED, errorType.getDescription());
    }

    public static Mono<Void> createForbiddenResponse(ServerWebExchange exchange, ErrorType errorType) {
        return createErrorResponse(exchange, HttpStatus.FORBIDDEN, errorType.getDescription());
    }

    public static Mono<Void> createErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        errorDetails.put("status", status.value());
        errorDetails.put("message", message);
        errorDetails.put("path", exchange.getRequest().getPath().value());

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorDetails);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            String fallbackMessage = "Error processing request: " + message;
            byte[] bytes = fallbackMessage.getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        }
    }
}
