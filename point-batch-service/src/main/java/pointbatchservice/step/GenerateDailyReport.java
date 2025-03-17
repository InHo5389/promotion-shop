package pointbatchservice.step;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import pointbatchservice.entity.DailyPointReport;
import pointbatchservice.entity.Point;
import pointbatchservice.entity.PointSummary;
import pointbatchservice.repository.DailyPointReportJpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenerateDailyReport {

    private static final int CHUNK_SIZE = 1000;

    private final JobRepository jobRepository;
    private final EntityManagerFactory entityManagerFactory;
    private final PlatformTransactionManager transactionManager;

    private final DailyPointReportJpaRepository dailyPointReportJpaRepository;

    /**
     * 일별 리포트 생성 Step
     * <p>
     * 전일 포인트 트랜잭션을 집계하여 일별 리포트를 생성하는 Step
     * - Reader: JPA별 통계 전일 포인트 트랜잭션 조회
     * - Processor: 포인트 트랜잭션을 사용자 별로 집계
     * - Writer: 일별 리포트를 DB에 저장
     */
    @Bean
    public Step generateDailyReportStep() {
        return new StepBuilder("generateDailyReportStep", jobRepository)
                .<Point, PointSummary>chunk(CHUNK_SIZE, transactionManager)
                .reader(pointReader())
                .processor(pointReportProcessor())
                .writer(reportWriter())
                .build();
    }

    /**
     * 전일의 포인트 트랜잭션 조회
     */
    @Bean
    @StepScope
    public JpaPagingItemReader<Point> pointReader() {
        HashMap<String, Object> parameters = new HashMap<>();
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        parameters.put("startTime", yesterday.withHour(0).withMinute(0).withSecond(0));
        parameters.put("endTime", yesterday.withHour(23).withMinute(59).withSecond(59));

        return new JpaPagingItemReaderBuilder<Point>()
                .name("pointReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(1000)
                .queryString("SELECT p FROM Point p WHERE p.createdAt BETWEEN :startTime AND :endTime")
                .parameterValues(parameters)
                .build();
    }

    /**
     * 포인트 트랜잭션을 사용자 별 정리 후 PointSummary 생성
     */
    @Bean
    @StepScope
    public ItemProcessor<Point, PointSummary> pointReportProcessor() {
        return point -> {
            switch (point.getType()) {
                case EARNED -> {
                    return new PointSummary(point.getUserId(), point.getAmount(), 0L, 0L);
                }
                case USED -> {
                    return new PointSummary(point.getUserId(), 0L, point.getAmount(), 0L);
                }
                case CANCELED -> {
                    return new PointSummary(point.getUserId(), 0L, 0L, point.getAmount());
                }
                default -> {
                    return null;
                }
            }
        };
    }

    @Bean
    @StepScope
    public ItemWriter<PointSummary> reportWriter() {
        return summaries -> {
            ArrayList<DailyPointReport> reports = new ArrayList<>();
            for (PointSummary summary : summaries) {
                DailyPointReport report = DailyPointReport.builder()
                        .userId(summary.getUserId())
                        .reportDates(LocalDate.now().minusDays(1))
                        .earnAmount(summary.getEarnAmount())
                        .useAmount(summary.getUseAmount())
                        .cancelAmount(summary.getCancelAmount())
                        .build();
                reports.add(report);
            }

            dailyPointReportJpaRepository.saveAll(reports);
        };
    }
}
