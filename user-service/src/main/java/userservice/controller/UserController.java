package userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import userservice.controller.dto.UserRequest;
import userservice.controller.dto.UserResponse;
import userservice.entity.User;
import userservice.entity.UserLoginHistory;
import userservice.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<?> create(@Valid @RequestBody UserRequest.Signup request) {
        User user = userService.createUser(request.getEmail(), request.getPassword(), request.getName());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserResponse.Response.from(user));
    }

    /**
     * Api Gateway에서 먼저 토큰에 대한 인증을 실시하고
     * 정상적인 경우 X-USER-ID를 헤더로 보냄
     */
    @GetMapping("/me")
    public ResponseEntity<?> getProfile(@RequestHeader("X-USER-ID") Long userId) {
        User user = userService.getUserById(userId);

        return ResponseEntity.ok(UserResponse.Response.from(user));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("X-USER-ID") Long userId,
            @Valid @RequestBody UserRequest.Update request) {
        User user = userService.updateUser(userId, request.getName());

        return ResponseEntity.ok(UserResponse.Response.from(user));
    }

    @PostMapping("/me/password")
    public ResponseEntity<?> changePassword(
            @RequestHeader("X-USER-ID") Long userId,
            @Valid @RequestBody UserRequest.ChangePassword request) {
        userService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());

        return ResponseEntity.ok().build();
    }

    @GetMapping("/me/login-history")
    public ResponseEntity<List<UserLoginHistory>> getLoginHistory(@RequestHeader("X-USER-ID") Long userId){

        return ResponseEntity.ok(userService.getUserLoginHistory(userId));
    }
}