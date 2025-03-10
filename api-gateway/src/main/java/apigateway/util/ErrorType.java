package apigateway.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorType {

    NOT_FOUND_HEADER("Authorization 헤더가 존재하지 않습니다."),
    NOT_AUTHENTICATION_USER("인증되지 않은 사용자 입니다. 로그인 후 사용하여 주세요"),
    NOT_AUTHORIZATION_USER("권한이 올바르지 않은 사용자입니다.");

    private final String description;
}
