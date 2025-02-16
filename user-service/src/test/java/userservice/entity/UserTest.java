package userservice.entity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("xxx 이름을 yyy로 변경한다")
    void changeName(){
        //given
        User user = User.builder()
                .name("xxx")
                .build();
        //when
        String changedName = "yyy";
        user.changeName(changedName);
        //then
        assertThat(user.getName()).isEqualTo(changedName);
    }

    @Test
    @DisplayName("password 비밀번호를 password!로 변경한다")
    void changePassword(){
        //given
        String currentPassword = "password";
        String newPassword = "password!";
        User user = User.builder()
                .password(currentPassword)
                .build();
        //when
        user.changePassword(newPassword);
        //then
        assertThat(user.getPassword()).isEqualTo(newPassword);
    }
}