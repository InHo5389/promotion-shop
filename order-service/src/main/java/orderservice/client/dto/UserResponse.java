//package orderservice.cleint.dto;
//
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//
//import java.time.LocalDateTime;
//
//public class UserResponse {
//
//    @Getter
//    @Builder
//    @NoArgsConstructor
//    @AllArgsConstructor
//    public static class Login{
//        private Long id;
//        private String email;
//        private String name;
//        private String token;
//        private LocalDateTime createdAt;
//        private LocalDateTime modifiedAt;
//
//        public static Login from(User user,String token){
//            return Login.builder()
//                    .id(user.getId())
//                    .email(user.getEmail())
//                    .name(user.getName())
//                    .token(token)
//                    .createdAt(user.getCreatedAt())
//                    .modifiedAt(user.getModifiedAt())
//                    .build();
//        }
//    }
//
//    @Getter
//    @Builder
//    @NoArgsConstructor
//    @AllArgsConstructor
//    public static class Response {
//        private Long id;
//        private String email;
//        private String name;
//        private LocalDateTime createdAt;
//        private LocalDateTime modifiedAt;
//
//        public static Response from(User user){
//            return Response.builder()
//                    .id(user.getId())
//                    .email(user.getEmail())
//                    .name(user.getName())
//                    .createdAt(user.getCreatedAt())
//                    .modifiedAt(user.getModifiedAt())
//                    .build();
//        }
//    }
//
//    @Getter
//    @Builder
//    @NoArgsConstructor
//    @AllArgsConstructor
//    public static class Token{
//        private Long id;
//        private String email;
//        private boolean valid;
//        private String role;
//
//        public static Token from(Claims claims){
//            return Token.builder()
//                    .id(claims.get("id",Long.class))
//                    .email(claims.getSubject())
//                    .valid(true)
//                    .role(claims.get("role",String.class))
//                    .build();
//        }
//    }
//}
