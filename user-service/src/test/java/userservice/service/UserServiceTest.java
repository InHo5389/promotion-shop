package userservice.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import userservice.common.exception.CustomGlobalException;
import userservice.common.exception.ErrorType;
import userservice.entity.User;
import userservice.repository.UserLoginHistoryRepository;
import userservice.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserLoginHistoryRepository userLoginHistoryRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원가입 시 유저 이메일이 이미 존재할 시 예외가 발생한다.")
    void createUser_alreadyExistUser() {
        //given
        String email = "email";
        String password = "password";
        String name = "name";
        User user = User.builder().email(email).name(name).password(password).build();

        Optional<User> optionalUser = Optional.of(user);
        given(userRepository.findByEmail(email)).willReturn(optionalUser);
        //when
        //then
        assertThatThrownBy(() -> userService.createUser(email, password, name))
                .isInstanceOf(CustomGlobalException.class)
                .hasMessage(ErrorType.ALREADY_EXIST_USER.getMessage());
    }

    @Test
    @DisplayName("회원가입 시 비밀번호를 암호화하여 저장한다.")
    void createUser() {
        //given
        String email = "email";
        String password = "password";
        String name = "name";
        String encodePassword = "encodePassword";

        given(userRepository.findByEmail(email)).willReturn(Optional.empty());
        given(passwordEncoder.encode(password)).willReturn(encodePassword);
        //when
        User user = userService.createUser(email, password, name);
        //then
        assertThat(user).extracting("email", "name", "password")
                .containsExactlyInAnyOrder(email, name, encodePassword);
    }

    @Test
    @DisplayName("로그인 시 비밀번호가 맞지 않으면 예외가 발생한다.")
    void unAuthenticate() {
        //given
        String email = "email";
        String password = "password";
        String name = "name";
        User user = User.builder().email(email).name(name).password(password).build();

        Optional<User> optionalUser = Optional.of(user);
        given(userRepository.findByEmail(email)).willReturn(optionalUser);
        given(passwordEncoder.matches(password, password)).willReturn(false);
        //when
        //then
        assertThatThrownBy(() -> userService.authenticate(email, password))
                .isInstanceOf(CustomGlobalException.class)
                .hasMessage(ErrorType.NON_AUTHORIZE_USER.getMessage());
    }

    @Test
    @DisplayName("로그인 시 인증되었으면 유저를 반환한다.")
    void authenticate() {
        //given
        String email = "email";
        String password = "password";
        String name = "name";
        User user = User.builder().email(email).name(name).password(password).build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(password, password)).willReturn(true);
        //when
        User authenticateUser = userService.authenticate(email, password);
        //then
        assertThat(user).isEqualTo(authenticateUser);
    }

    @Test
    @DisplayName("userId를 통하여 이름을 변경할 수 있다.")
    void updateUser() {
        //given
        Long userId = 1L;
        String name = "name";
        String changeName = "changeName";
        User user = User.builder().email("email").name(name).password("password").build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        //when
        User updateUser = userService.updateUser(userId, changeName);
        //then
        assertThat(updateUser.getName()).isEqualTo(changeName);
    }

    @Test
    @DisplayName("비밀번호를 변경할때 현재 패스워드와 입력받은 패스워드가 다르면 예외가 발생한다..")
    void changePassword() {
        //given
        Long userId = 1L;
        String currentPassword = "currentPassword";
        String newPassword = "newPassword";
        User user = User.builder().email("email").name("name").password(currentPassword).build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        //when
        //then
        assertThatThrownBy(() -> userService.changePassword(userId, "password", newPassword))
                .isInstanceOf(CustomGlobalException.class)
                .hasMessage(ErrorType.NON_AUTHORIZE_USER.getMessage());
    }
}