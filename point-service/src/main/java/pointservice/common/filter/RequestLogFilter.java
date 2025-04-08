package pointservice.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLogFilter extends OncePerRequestFilter {

    private static final Logger apiLogger = LoggerFactory.getLogger("api-log");
    private static final String TRACE_ID = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(TRACE_ID,traceId);

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            // 요청 정보 로깅 (경로, 메소드, 헤더 등)
            logRequest(requestWrapper);

            // 필터 체인 실행
            filterChain.doFilter(requestWrapper, responseWrapper);

            // 응답 정보 로깅
            long endTime = System.currentTimeMillis();
            logResponse(responseWrapper, endTime - startTime);

        } finally {
            // 응답 데이터를 복사한 후 클라이언트에게 전송
            responseWrapper.copyBodyToResponse();
            // MDC 정리
            MDC.remove(TRACE_ID);
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        String userId = request.getHeader("X-USER-ID");
        String queryString = request.getQueryString() != null ? "?" + request.getQueryString() : "";

        apiLogger.info(
                "REQUEST [{}] {} {}{} - USER_ID: {}",
                request.getMethod(),
                request.getRequestURI(),
                queryString,
                request.getContentType() != null ? " (" + request.getContentType() + ")" : "",
                userId != null ? userId : "ANONYMOUS"
        );

        // 요청 본문 로깅 (POST/PUT 요청 등)
        if (shouldLogRequestBody(request)) {
            String requestBody = new String(request.getContentAsByteArray());
            if (!requestBody.isEmpty()) {
                apiLogger.debug("REQUEST BODY: {}", requestBody);
            }
        }
    }

    private void logResponse(ContentCachingResponseWrapper response, long duration) {
        int status = response.getStatus();

        // 응답 로깅 (상태 코드, 응답 시간)
        apiLogger.info(
                "RESPONSE {} - {}ms",
                status,
                duration
        );

        // 에러 상태 코드인 경우 응답 본문 로깅
        if (status >= 400) {
            String responseBody = new String(response.getContentAsByteArray());
            if (!responseBody.isEmpty()) {
                apiLogger.warn("RESPONSE BODY (Error): {}", responseBody);
            }
        }
    }

    private boolean shouldLogRequestBody(HttpServletRequest request) {
        String method = request.getMethod();
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }
}
