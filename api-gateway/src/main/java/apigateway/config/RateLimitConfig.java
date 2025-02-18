//package apigateway.config;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
//import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import reactor.core.publisher.Mono;
//
//@Slf4j
//@Configuration
//public class RateLimitConfig {
//
//    @Bean
//    public RedisRateLimiter redisRateLimiter() {
//        // replenishRate : 초당 허용 수
//        // burstCapacity : 최대 누적 가능한 요청 수
//        RedisRateLimiter redisRateLimiter = new RedisRateLimiter(10, 20);
//        redisRateLimiter.setIncludeHeaders(true);
//        return redisRateLimiter;
//    }
//
//    /**
//     * KeyResolver는 요청이 계속 들어왔을때 어떤 부분을 key값으로 잡고 요청을 count하는 부분
//     * 유저별로 제한하기 위해서 X-USER-ID 헤더에서 userId가 있으면 사용을 하고 없으면
//     * ip주소로 제한
//     */
//    @Bean
//    public KeyResolver userKeyResolver() {
//        return exchange -> {
//            String key = exchange.getRequest().getHeaders().getFirst("X-USER-ID") != null ?
//                    exchange.getRequest().getHeaders().getFirst("X-USER-ID") :
//                    exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
//
//            log.info("Rate Limit Key: {}", key);  // 로그 추가
//            return Mono.just(key);
//        };
//    }
//}
