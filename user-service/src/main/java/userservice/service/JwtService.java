package userservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import userservice.common.exception.CustomGlobalException;
import userservice.common.exception.ErrorType;
import userservice.entity.User;
import userservice.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Value("${jwt.secret}")
    private String secretKey;

    public String generateToken(User user) {
        log.info("JWT 토큰 생성 - userId: {}, email: {}", user.getId(), user.getEmail());

        long currentTimeMillis = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("id", user.getId())
                .claim("role", user.getRole())
                .issuedAt(new Date(currentTimeMillis))
                .expiration(new Date(currentTimeMillis + 3600000)) // 1시간
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    public Claims validateToken(String token) {
        log.debug("JWT 토큰 검증 시도");

        try {
            Claims claims = parseJwtClaims(token);
            log.info("JWT 토큰 검증 성공 - subject: {}, userId: {}",
                    claims.getSubject(), claims.get("id"));
            return claims;
        } catch (Exception e) {
            log.error("Token validation error : ", e);
            throw new IllegalArgumentException("Invalid Token");
        }
    }

    private Claims parseJwtClaims(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Transactional
    public String refreshToken(String token) {
        log.info("JWT 토큰 갱신 요청");

        Claims claims = parseJwtClaims(token);
        long currentTimeMillis = System.currentTimeMillis();

        log.info("JWT 토큰 갱신 성공 - subject: {}, userId: {}",
                claims.getSubject(), claims.get("id", Long.class));

        String refreshToken = Jwts.builder()
                .subject(claims.getSubject())
                .claims(claims)
                .issuedAt(new Date(currentTimeMillis))
                .expiration(new Date(currentTimeMillis +  + 7 * 24 * 60 * 60 * 1000L)) // 1주일
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
                .compact();

        Long id = claims.get("id", Long.class);
        User user = userRepository.findById(id).orElseThrow(
                () -> new CustomGlobalException(ErrorType.NOT_FOUND_USER));
        user.saveRefreshToken(refreshToken);

        return refreshToken;
    }
}
