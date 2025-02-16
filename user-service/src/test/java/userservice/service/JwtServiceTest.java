package userservice.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import userservice.entity.User;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp(){
        ReflectionTestUtils.setField(jwtService,"secretKey","yourTestSecretKeyMustBeAtLeast32BytesLong");
    }

    @Test
    @DisplayName("토큰 생성 테스트")
    void generateToken(){
        //given
        User user = User.builder()
                .email("test@xx.com")
                .password("password")
                .build();
        //when
        String token = jwtService.generateToken(user);
        Claims claims = jwtService.validateToken(token);
        //then
        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3);

        Date expirationDate = claims.getExpiration();
        Date issuedAt = claims.getIssuedAt();

        assertThat(expirationDate).isNotNull();
        assertThat(issuedAt).isNotNull();

        // 발급시간과 만료시간의 차이가 1시간(3600000 밀리초)인지 확인
        long diff = expirationDate.getTime() - issuedAt.getTime();
        assertThat(diff).isEqualTo(3600000);
    }

    @Test
    @DisplayName("유효한 토큰 검증 테스트")
    void validateToken(){
        //given
        String email = "test@xx.com";
        User user = User.builder()
                .email(email)
                .password("password")
                .build();
        String token = jwtService.generateToken(user);
        //when
        Claims claims = jwtService.validateToken(token);
        //then
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo(email);
        assertThat(claims.get("role")).isEqualTo("USER");
    }

    @Test
    @DisplayName("유효하지 않은 토큰 검증 테스트")
    void unValidateToken(){
        //given
        String invalidToken = "invalidToken";
        //when
        //then
        assertThatThrownBy(()->jwtService.validateToken(invalidToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid Token");
    }

    @Test
    @DisplayName("토큰을 받아서 refresh token 발급 테스트")
    void refreshToken() {
        // Given
        String email = "test@example.com";
        User user = User.builder()
                .email(email)
                .password("password")
                .build();
        String originalToken = jwtService.generateToken(user);

        // When
        String refreshedToken = jwtService.refreshToken(originalToken);

        // Then
        assertThat(refreshedToken)
                .isNotNull()
                .isNotEqualTo(originalToken);

        Claims refreshedClaims = jwtService.validateToken(refreshedToken);
        assertThat(refreshedClaims.getSubject()).isEqualTo(email);
        assertThat(refreshedClaims.get("role")).isEqualTo("USER");
    }
}