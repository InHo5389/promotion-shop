package userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import userservice.entity.User;
import userservice.entity.UserLoginHistory;

import java.util.List;

public interface UserLoginHistoryRepository extends JpaRepository<UserLoginHistory,Long> {
    List<UserLoginHistory> findByUserOrderByLoginTimeDesc(User user);
}
