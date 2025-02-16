package userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import userservice.controller.dto.UserRequest;
import userservice.entity.User;
import userservice.service.JwtService;
import userservice.service.UserService;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @Test
    @DisplayName("로그인 시 이메일, 비밀번호를 입력하면 200 응답한다.")
    void login() throws Exception {
        //given
        String email = "email@xxx.xxx";
        String password = "password";
        UserRequest.Login request = UserRequest.Login.builder()
                .email(email)
                .password(password)
                .build();

        User mockUser = User.builder()
                .id(1L)
                .email(email)
                .build();

        given(userService.authenticate(email, password))
                .willReturn(mockUser);

        given(jwtService.generateToken(mockUser))
                .willReturn("test.token");
        //when
        //then
        mockMvc.perform(
                        post("/api/v1/users/login")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("로그인 시 이메일은 필수 요구사항이라 없으면 예외가 발생한다.")
    void loginWithEmptyEmail() throws Exception {
        //given
        UserRequest.Login request = UserRequest.Login.builder()
                .password("password")
                .build();
        //when
        //then
        mockMvc.perform(
                post("/api/v1/users/login")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
        )
                .andDo(print())
                .andExpect(jsonPath("$.message").value("이메일은 필수 요구사항입니다."))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인 시 비밀번호는 필수 요구사항이라 없으면 예외가 발생한다.")
    void loginWithEmptyPassword() throws Exception {
        //given
        UserRequest.Login request = UserRequest.Login.builder()
                .email("email@xxx.xxx")
                .build();
        //when
        //then
        mockMvc.perform(
                post("/api/v1/users/login")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
        )
                .andDo(print())
                .andExpect(jsonPath("$.message").value("비밀번호는 필수 요구사항입니다."))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인 시 이메일형식을 지키지 않으면 예외가 발생한다.")
    void loginWithInvalidEmailFormat() throws Exception {
        //given
        UserRequest.Login request = UserRequest.Login.builder()
                .email("email")
                .password("password")
                .build();
        //when
        //then
        mockMvc.perform(
                post("/api/v1/users/login")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
        )
                .andDo(print())
                .andExpect(jsonPath("$.message").value("이메일 형식을 지키셔야 합니다."))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("토큰을 검증할 때 정상적이면 200 응답한다.")
    void validateToken() throws Exception {
        //given
        String token = "token";
        UserRequest.Token request = UserRequest.Token.builder()
                .token(token)
                .build();

        Claims mockClaims = Jwts.claims()
                .subject("test@example.com")
                .build();

        // JwtService의 validateToken 메서드가 호출될 때 mockClaims를 반환하도록 설정
        given(jwtService.validateToken(token))
                .willReturn(mockClaims);

        //when
        //then
        mockMvc.perform(
                post("/api/v1/users/validate-token")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
        )
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("토큰을 검증할 때 토큰이 오지 않으면 예외가 발생한다.")
    void validateTokenEmptyToken() throws Exception {
        //given
        UserRequest.Token request = UserRequest.Token.builder()
                .build();
        //when
        //then
        mockMvc.perform(
                post("/api/v1/users/validate-token")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
        )
                .andDo(print())
                .andExpect(jsonPath("$.message").value("토큰은 필수 요구사항입니다."))
                .andExpect(status().isBadRequest());
    }
}