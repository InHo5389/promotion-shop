package userservice.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "user_login_histories")
public class UserLoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime loginTime;
    private String ipAddress;
}
