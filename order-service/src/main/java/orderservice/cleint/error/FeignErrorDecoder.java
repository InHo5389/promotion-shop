package orderservice.cleint.error;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Component
public class FeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        HttpStatus status = HttpStatus.valueOf(response.status());

        try {
            switch (status) {
                case NOT_FOUND:
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다: " + response.reason());
                case BAD_REQUEST:
                    return new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 요청입니다: " + response.reason());
                default:
                    return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "상품 서비스 오류: " + response.reason());
            }
        }catch (Exception e) {
            log.error("Error reading Feign response body", e);
            return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "응답 처리 중 오류 발생: " + e.getMessage());
        }
    }
}
