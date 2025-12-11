package userservice.controller.dto;

import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import userservice.entity.User;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class UserResponse {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Login{
        private Long id;
        private String email;
        private String name;
        private String accessToken;
        private String refreshToken;
        private LocalDateTime createdAt;
        private LocalDateTime modifiedAt;

        public static Login from(User user,String accessToken,String refreshToken){
            return Login.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .createdAt(user.getCreatedAt())
                    .modifiedAt(user.getModifiedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String email;
        private String name;
        private LocalDateTime createdAt;
        private LocalDateTime modifiedAt;

        public static Response from(User user){
            return Response.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .createdAt(user.getCreatedAt())
                    .modifiedAt(user.getModifiedAt())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Token {
        private Map<String, Object> claims;

        public static Token from(Claims claims) {
            // Claims를 Map으로 변환
            Map<String, Object> claimsMap = new HashMap<>();
            claimsMap.put("id",claims.get("id"));
            claimsMap.put("role",claims.get("role"));

            return new Token(claimsMap);
        }
    }
}
