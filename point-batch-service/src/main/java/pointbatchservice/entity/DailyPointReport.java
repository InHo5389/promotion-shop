package pointbatchservice.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "daily_point_reports")
public class DailyPointReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate reportDates;

    @Column(nullable = false)
    private Long earnAmount;

    @Column(nullable = false)
    private Long useAmount;

    @Column(nullable = false)
    private Long cancelAmount;

    @Column(nullable = false)
    private Long netAmount;

    @Builder
    public DailyPointReport(Long userId, LocalDate reportDates, Long earnAmount,
                            Long useAmount, Long cancelAmount) {
        this.userId = userId;
        this.reportDates = reportDates;
        this.earnAmount = earnAmount;
        this.useAmount = useAmount;
        this.cancelAmount = cancelAmount;
        this.netAmount = earnAmount - useAmount + cancelAmount;
    }
}
