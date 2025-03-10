package userservice.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {
    USER("유저"),
    SELLER("판매자"),
    ADMIN("관리자");

    private final String description;
}
