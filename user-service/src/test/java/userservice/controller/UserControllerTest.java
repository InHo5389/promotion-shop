package userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import userservice.controller.dto.UserRequest;
import userservice.service.UserService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("회원가입 시 이메일은 필수 요구사항이라 없으면 예외가 발생한다.")
    void loginWithEmptyEmail() throws Exception {
        //given
        UserRequest.Signup request = UserRequest.Signup.builder()
                .name("name")
                .password("password1234!")
                .build();
        //when
        //then
        mockMvc.perform(
                        post("/api/v1/users/signup")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(jsonPath("$.message").value("이메일은 필수 요구사항입니다."))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("회원가입 시 이메일의 형식을 지키지 않으면 예외가 발생한다.")
    void loginWithInvalidEmail() throws Exception {
        //given
        UserRequest.Signup request = UserRequest.Signup.builder()
                .email("email")
                .name("name")
                .password("password1234!")
                .build();
        //when
        //then
        mockMvc.perform(
                        post("/api/v1/users/signup")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(jsonPath("$.message").value("이메일 형식을 지키셔야 합니다."))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비밀번호가 8자 미만이면 예외가 발생한다.")
    void validatePassword_LengthLessThanEight() throws Exception {
        //given
        UserRequest.Signup request = UserRequest.Signup.builder()
                .email("email@xxx.xxx")
                .name("name")
                .password("passwor")
                .build();
        //when
        //then
        mockMvc.perform(
                        post("/api/v1/users/signup")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(jsonPath("$.message").value("비밀번호는 최소 8자 이상, 최소 하나의 영문자, 최소 하나의 숫자, 최소 하나의 특수문자를 포함해야 합니다."))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비밀번호에 하나이상의 영문자가 들어가지 않으면 예외가 발생한다.")
    void validatePassword_NoLetter() throws Exception {
        //given
        UserRequest.Signup request = UserRequest.Signup.builder()
                .email("email@xxx.xxx")
                .name("name")
                .password("12345678")
                .build();
        //when
        //then
        mockMvc.perform(
                        post("/api/v1/users/signup")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(jsonPath("$.message").value("비밀번호는 최소 8자 이상, 최소 하나의 영문자, 최소 하나의 숫자, 최소 하나의 특수문자를 포함해야 합니다."))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비밀번호에 하나이상의 숫자가 들어가지 않으면 예외가 발생한다.")
    void validatePassword_NoNumber() throws Exception {
        //given
        UserRequest.Signup request = UserRequest.Signup.builder()
                .email("email@xxx.xxx")
                .name("name")
                .password("password")
                .build();
        //when
        //then
        mockMvc.perform(
                        post("/api/v1/users/signup")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(jsonPath("$.message").value("비밀번호는 최소 8자 이상, 최소 하나의 영문자, 최소 하나의 숫자, 최소 하나의 특수문자를 포함해야 합니다."))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비밀번호에 하나이상의 특수문자가 들어가지 않으면 예외가 발생한다.")
    void validatePassword_NoSpecialCharacter() throws Exception {
        //given
        UserRequest.Signup request = UserRequest.Signup.builder()
                .email("email@xxx.xxx")
                .name("name")
                .password("pass1234")
                .build();
        //when
        //then
        mockMvc.perform(
                        post("/api/v1/users/signup")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(jsonPath("$.message").value("비밀번호는 최소 8자 이상, 최소 하나의 영문자, 최소 하나의 숫자, 최소 하나의 특수문자를 포함해야 합니다."))
                .andExpect(status().isBadRequest());
    }
}