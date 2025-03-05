package orderservice.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import orderservice.common.exception.CustomGlobalException;
import orderservice.common.exception.ErrorType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserIdInterceptor implements HandlerInterceptor {

    private static final String USER_ID_HEADER = "X-USER-ID";
    private static final ThreadLocal<Long> currentUserId = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String userIdStr = request.getHeader(USER_ID_HEADER);

        if (userIdStr == null || userIdStr.isEmpty()) {
            throw new CustomGlobalException(ErrorType.NOT_FOUND_X_USER_ID_HEADER);
        }
        try {
            currentUserId.set(Long.parseLong(userIdStr));
            return true;
        }catch (NumberFormatException e){
            throw new IllegalStateException("Invalid X-USER-ID format");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        currentUserId.remove();
    }

    public static Long getCurrentUserId(){
        Long userId = currentUserId.get();
        if (userId == null){
            throw new IllegalStateException();
        }
        return userId;
    }

    public static void setTestUserId(Long userId) {
        currentUserId.set(userId);
    }

    public static void clearTestUserId() {
        currentUserId.remove();
    }
}
