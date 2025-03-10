package apigateway.filter;

import apigateway.util.ErrorResponseUtil;
import apigateway.util.ErrorType;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class RoleAuthorizationFilter extends AbstractGatewayFilterFactory<RoleAuthorizationFilter.Config> {

    public static final String HEADER_ROLE_NAME = "X-USER-ROLE";

    public RoleAuthorizationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(RoleAuthorizationFilter.Config config) {
        return ((exchange, chain) -> {
            String userRole = exchange.getRequest().getHeaders().getFirst(HEADER_ROLE_NAME);

            if (userRole == null) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return ErrorResponseUtil.createUnauthorizedResponse(exchange, ErrorType.NOT_FOUND_HEADER);
            }

            String[] requiredRoles = config.getRoles().split(",");
            boolean hasRequiredRole = false;

            for (String role : requiredRoles) {
                if (userRole.equals(role.trim())) {
                    hasRequiredRole = true;
                    break;
                }
            }

            if (!hasRequiredRole) {
                return ErrorResponseUtil.createForbiddenResponse(exchange, ErrorType.NOT_AUTHORIZATION_USER);
            }

            return chain.filter(exchange);
        });
    }

    @Data
    @NoArgsConstructor
    public static class Config {
        private String roles;
    }
}
