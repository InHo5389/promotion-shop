package orderservice.common.config;

import feign.RequestInterceptor;
import feign.Retryer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // 모든 Feign 요청에 X-USER-ID 헤더 추가
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
                String userId = request.getHeader("X-USER-ID");
                if (userId != null) {
                    requestTemplate.header("X-USER-ID", userId);
                }
            }
        };
    }

    @Bean
    public Retryer retryer() {
        return Retryer.NEVER_RETRY;
    }
}