package userservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import userservice.entity.User;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final PasswordEncoder passwordEncoder;

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

    public String refreshToken(String token) {
        log.info("JWT 토큰 갱신 요청");

        Claims claims = parseJwtClaims(token);
        long currentTimeMillis = System.currentTimeMillis();

        log.info("JWT 토큰 갱신 성공 - subject: {}, userId: {}",
                claims.getSubject(), claims.get("id", Long.class));

        return Jwts.builder()
                .subject(claims.getSubject())
                .claims(claims)
                .issuedAt(new Date(currentTimeMillis))
                .expiration(new Date(currentTimeMillis + 3600000)) // 1시간
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
