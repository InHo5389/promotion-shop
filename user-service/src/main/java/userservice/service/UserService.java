package userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import userservice.common.exception.CustomGlobalException;
import userservice.common.exception.ErrorType;
import userservice.entity.User;
import userservice.entity.UserLoginHistory;
import userservice.repository.UserLoginHistoryRepository;
import userservice.repository.UserRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserLoginHistoryRepository userLoginHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    public User createUser(String email, String password, String name) {
        log.info("사용자 생성 요청 - email: {}, name: {}", email, name);

        if (userRepository.findByEmail(email).isPresent()) {
            throw new CustomGlobalException(ErrorType.ALREADY_EXIST_USER);
        }

        User user = User.create(email, passwordEncoder.encode(password), name);
        User savedUser = userRepository.save(user);

        log.info("사용자 생성 성공 - userId: {}, email: {}", savedUser.getId(), email);
        return savedUser;
    }

    public User authenticate(String email, String password) {
        log.info("사용자 인증 요청 - email: {}", email);

        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new CustomGlobalException(ErrorType.NOT_FOUND_USER));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.info("인증 실패 - 비밀번호 불일치 - userId: {}, email: {}", user.getId(), email);
            throw new CustomGlobalException(ErrorType.NON_AUTHORIZE_USER);
        }

        log.info("사용자 인증 성공 - userId: {}, email: {}", user.getId(), email);
        return user;
    }

    public User getUserById(Long userId) {
        log.info("사용자 정보 조회 - userId: {}", userId);

        return userRepository.findById(userId).orElseThrow(
                () -> new CustomGlobalException(ErrorType.NOT_FOUND_USER));
    }

    @Transactional
    public User updateUser(Long userId, String name) {
        log.info("사용자 정보 업데이트 요청 - userId: {}, newName: {}", userId, name);

        User user = getUserById(userId);
        user.changeName(name);

        log.info("사용자 정보 업데이트 성공 - userId: {}", userId);
        return user;
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        log.info("비밀번호 변경 요청 - userId: {}", userId);

        User user = getUserById(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            log.info("비밀번호 변경 실패 - 현재 비밀번호 불일치 - userId: {}", userId);
            throw new CustomGlobalException(ErrorType.NON_AUTHORIZE_USER);
        }

        user.changePassword(newPassword);
        log.info("비밀번호 변경 성공 - userId: {}", userId);
    }

    public List<UserLoginHistory> getUserLoginHistory(Long userId){
        log.info("사용자 로그인 내역 조회 - userId: {}", userId);

        User user = getUserById(userId);
        return userLoginHistoryRepository.findByUserOrderByLoginTimeDesc(user);
    }
}
