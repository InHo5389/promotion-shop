package apigateway.filter;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    // webClient 추가하는 이유는 gateway에서 유저서비스를 통해 jwt를 검증하거나 유저정보를 가져오는것을 구현하기 위하여
    @LoadBalanced
    private final WebClient webClient;

    public JwtAuthenticationFilter(ReactorLoadBalancerExchangeFilterFunction lbFunction) {
        super(Config.class);
        this.webClient = WebClient.builder()
                .filter(lbFunction)
                .baseUrl("http://user-service")
                .build();
    }

    /**
     * 해당 라우터를 타게되면 yml에 있는 user-service에 들어올때 JwtAuthenticationFilter를 타는데
     * 필터르 타고 apply로직 수행
     */
    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                return validateToken(token)
                        .flatMap(userId-> proceedWithUserId(userId,exchange,chain))
                        .switchIfEmpty(chain.filter(exchange))
                        .onErrorResume(e-> handleAuthenticationError(exchange,e));
            }
            return chain.filter(exchange);
        });
    }

    /**
     * 추후 JsonBuilder 사용
     * 요청했을때 map으로 변환하여 id값을 가져오는 로직
     */
    private Mono<Long> validateToken(String token) {
        return webClient.post()
                .uri("/api/v1/users/validate-token")
                .bodyValue("{\"token\":\""+token+"\"}")
                .header("Content-Type","application/json")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response->Long.valueOf(response.get("id").toString()));
    }

    /**
     * userId를 가지고 exchange에 헤더를 넣어주는 작업
     * 추후 백엔드 컴포넌트 X-USER_ID헤더에 userId를 넣어줌
     */
    private Mono<Void> proceedWithUserId(Long userId, ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getRequest().mutate().header("X-USER_ID",String.valueOf(userId));
        return chain.filter(exchange);
    }

    private Mono<Void> handleAuthenticationError(ServerWebExchange exchange, Throwable e) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
        // 필터 구성을 위한 설정 클래스
    }
}
