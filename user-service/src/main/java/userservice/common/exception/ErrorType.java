package userservice.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorType {

    ALREADY_EXIST_USER(400,"이미 등록된 유저입니다."),
    NOT_FOUND_USER(400,"유저를 찾을 수 없습니다."),
    NON_AUTHORIZE_USER(400,"비밀번호가 일치하지 않아 인증되지 않은 사용자입니다");

    private final int status;
    private final String message;
}
