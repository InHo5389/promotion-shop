package userservice.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserRequest {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Login{
        @NotBlank(message = "이메일은 필수 요구사항입니다.")
        @Email(message = "이메일 형식을 지키셔야 합니다.")
        private String email;

        @NotBlank(message = "비밀번호는 필수 요구사항입니다.")
        private String password;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Token {

        @NotBlank(message = "토큰은 필수 요구사항입니다.")
        private String token;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Signup {
        @NotBlank(message = "이메일은 필수 요구사항입니다.")
        @Email(message = "이메일 형식을 지키셔야 합니다.")
        private String email;

        @NotBlank(message = "이름은 필수 요구사항입니다.")
        private String name;

        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
                message = "비밀번호는 최소 8자 이상, 최소 하나의 영문자, 최소 하나의 숫자, 최소 하나의 특수문자를 포함해야 합니다."
        )
        @NotBlank(message = "비밀번호는 필수 요구사항입니다.")
        private String password;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Update {
        @NotBlank(message = "이름은 필수 요구사항입니다.")
        private String name;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangePassword {
        @NotBlank(message = "이름은 필수 요구사항입니다.")
        private String currentPassword;
        private String newPassword;
    }
}
