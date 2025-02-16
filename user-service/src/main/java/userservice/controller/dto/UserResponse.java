package userservice.controller.dto;

import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import userservice.entity.User;

import java.time.LocalDateTime;

public class UserResponse {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Login{
        private Long id;
        private String email;
        private String name;
        private String token;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Login from(User user,String token){
            return Login.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .token(token)
                    .createdAt(user.getCreatedAt())
                    .updatedAt(user.getUpdatedAt())
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
        private LocalDateTime updatedAt;

        public static Login from(User user){
            return Login.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .createdAt(user.getCreatedAt())
                    .updatedAt(user.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Token{
        private String email;
        private boolean valid;
        private String role;

        public static Token from(Claims claims){
            return Token.builder()
                    .email(claims.getSubject())
                    .valid(true)
                    .role(claims.get("role",String.class))
                    .build();
        }
    }
}
