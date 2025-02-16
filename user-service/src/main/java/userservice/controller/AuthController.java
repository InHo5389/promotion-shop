package userservice.controller;

import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import userservice.controller.dto.UserRequest;
import userservice.controller.dto.UserResponse;
import userservice.entity.User;
import userservice.service.JwtService;
import userservice.service.UserService;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService jwtService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody UserRequest.Login request) {
        User user = userService.authenticate(request.getEmail(), request.getPassword());
        String token = jwtService.generateToken(user);

        return ResponseEntity.ok(UserResponse.Login.from(user, token));
    }

    @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken(@Valid @RequestBody UserRequest.Token request) {
        Claims claims = jwtService.validateToken(request.getToken());

        return ResponseEntity.ok(UserResponse.Token.from(claims));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<Map<String, String>> refreshToken(@Valid @RequestBody UserRequest.Token request) {
        String newToken = jwtService.refreshToken(request.getToken());

        return ResponseEntity.ok(Collections.singletonMap("token", newToken));
    }
}
