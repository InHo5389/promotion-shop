package pointbatchservice.step.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import pointbatchservice.entity.DailyPointReport;
import pointbatchservice.entity.Point;
import pointbatchservice.entity.PointSummary;
import pointbatchservice.entity.PointType;
import pointbatchservice.repository.DailyPointReportJpaRepository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenerateDailyReportV2 {

    private static final int CHUNK_SIZE = 5000;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;
    private final DailyPointReportJpaRepository dailyPointReportJpaRepository;

    /**
     * 일별 리포트 생성 Step
     * <p>
     * 전일 포인트 트랜잭션을 집계하여 일별 리포트를 생성하는 Step
     * - Reader: JDBC Cursor로 전일 포인트 트랜잭션 조회
     * - Processor: 포인트 트랜잭션을 사용자 별로 집계
     * - Writer: 일별 리포트를 DB에 저장
     */
    @Bean
    public Step generateDailyReportStepV2() {
        return new StepBuilder("generateDailyReportStepV2", jobRepository)
                .<Point, PointSummary>chunk(CHUNK_SIZE, transactionManager)
                .reader(pointReaderV2())
                .processor(pointReportProcessorV2())
                .writer(reportWriterV2())
                .build();
    }

    /**
     * 전일의 포인트 트랜잭션 조회 - JDBC Cursor 방식
     */
    @Bean
    @StepScope
    public JdbcCursorItemReader<Point> pointReaderV2() {

         LocalDateTime startTime = LocalDateTime.now().minusDays(1).withHour(0).withMinute(0).withSecond(0);
         LocalDateTime endTime = LocalDateTime.now().minusDays(1).withHour(23).withMinute(59).withSecond(59);

        log.info("Point transaction retrieval period: {} ~ {}", startTime, endTime);

        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("startTime", startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        parameterValues.put("endTime", endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        return new JdbcCursorItemReaderBuilder<Point>()
                .name("pointReaderV2")
                .dataSource(dataSource)
                .sql("SELECT id, user_id, amount, type, balance_snapshot, created_at, point_balance_id, version " +
                        "FROM points " +
                        "WHERE created_at BETWEEN ? AND ?")
                .preparedStatementSetter(ps -> {
                    ps.setFetchSize(Integer.MIN_VALUE);
                    ps.setString(1, startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    ps.setString(2, endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                })
                .fetchSize(CHUNK_SIZE)
                .verifyCursorPosition(false)
                .rowMapper((rs, rowNum) -> mapResultSetToPoint(rs))
                .build();
    }

    private Point mapResultSetToPoint(ResultSet rs) throws SQLException {
        return Point.builder()
                .id(rs.getLong("id"))
                .userId(rs.getLong("user_id"))
                .amount(rs.getLong("amount"))
                .type(PointType.valueOf(rs.getString("type")))
                .balanceSnapshot(rs.getLong("balance_snapshot"))
                .version(rs.getLong("version"))
                .build();
    }

    /**
     * 포인트 트랜잭션을 사용자 별 정리 후 PointSummary 생성
     */
    @Bean
    @StepScope
    public ItemProcessor<Point, PointSummary> pointReportProcessorV2() {
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
    public ItemWriter<PointSummary> reportWriterV2() {
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

            log.info("Writing batch of {} reports", reports.size());
            dailyPointReportJpaRepository.saveAll(reports);
        };
    }
}