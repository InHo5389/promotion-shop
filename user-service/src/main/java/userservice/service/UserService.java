package userservice.service;

import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserLoginHistoryRepository userLoginHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    public User createUser(String email, String password, String name) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new CustomGlobalException(ErrorType.ALREADY_EXIST_USER);
        }

        User user = User.create(email, passwordEncoder.encode(password), name);
        User savedUser = userRepository.save(user);
        return savedUser;
    }

    public User authenticate(String email, String password) {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new CustomGlobalException(ErrorType.NOT_FOUND_USER));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new CustomGlobalException(ErrorType.NON_AUTHORIZE_USER);
        }

        return user;
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new CustomGlobalException(ErrorType.NOT_FOUND_USER));
    }

    @Transactional
    public User updateUser(Long userId, String name) {
        User user = getUserById(userId);
        user.changeName(name);
        return user;
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getUserById(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new CustomGlobalException(ErrorType.NON_AUTHORIZE_USER);
        }

        user.changePassword(newPassword);
    }

    public List<UserLoginHistory> getUserLoginHistory(Long userId){
        User user = getUserById(userId);
        return userLoginHistoryRepository.findByUserOrderByLoginTimeDesc(user);
    }
}
