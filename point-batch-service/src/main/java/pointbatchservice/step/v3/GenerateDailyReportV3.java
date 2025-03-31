package pointbatchservice.step.v3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import pointbatchservice.entity.Point;
import pointbatchservice.entity.PointSummary;
import pointbatchservice.entity.PointType;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenerateDailyReportV3 {

    private static final int CHUNK_SIZE = 5000;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;

    /**
     * 일별 리포트 생성 Step
     * <p>
     * 전일 포인트 트랜잭션을 집계하여 일별 리포트를 생성하는 Step
     * - Reader: JDBC Cursor로 전일 포인트 트랜잭션 조회
     * - Processor: 포인트 트랜잭션을 사용자 별로 집계
     * - Writer: 일별 리포트를 DB에 저장
     */
    @Bean
    public Step generateDailyReportStepV3() {
        return new StepBuilder("generateDailyReportStepV3", jobRepository)
                .<Point, PointSummary>chunk(CHUNK_SIZE, transactionManager)
                .reader(pointReaderV3())
                .processor(pointReportProcessorV3())
                .writer(reportWriterV3())
                .build();
    }

    /**
     * 전일의 포인트 트랜잭션 조회 - JDBC Cursor 방식
     */
    @Bean
    @StepScope
    public JdbcCursorItemReader<Point> pointReaderV3() {
         LocalDateTime startTime = LocalDateTime.now().minusDays(1).withHour(0).withMinute(0).withSecond(0);
         LocalDateTime endTime = LocalDateTime.now().minusDays(1).withHour(23).withMinute(59).withSecond(59);

        log.info("Point transaction retrieval period: {} ~ {}", startTime, endTime);

        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("startTime", startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        parameterValues.put("endTime", endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        return new JdbcCursorItemReaderBuilder<Point>()
                .name("pointReaderV3")
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
    public ItemProcessor<Point, PointSummary> pointReportProcessorV3() {
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
    public ItemWriter<PointSummary> reportWriterV3() {

        return new JdbcBatchItemWriterBuilder<PointSummary>()
                .dataSource(dataSource)
                .sql("""
                        INSERT INTO daily_point_reports (
                            user_id,
                            report_dates,
                            earn_amount,
                            use_amount,
                            cancel_amount,
                            net_amount
                        ) VALUES (?, ?, ?, ?, ?, ?)
                        """)
                .itemPreparedStatementSetter((item, ps) -> {
                    LocalDate reportDate = LocalDate.now().minusDays(1);
                    long netAmount = item.getEarnAmount() - item.getUseAmount() + item.getCancelAmount();

                    ps.setLong(1, item.getUserId());
                    ps.setDate(2, java.sql.Date.valueOf(reportDate));
                    ps.setLong(3, item.getEarnAmount());
                    ps.setLong(4, item.getUseAmount());
                    ps.setLong(5, item.getCancelAmount());
                    ps.setLong(6, netAmount);
                })
                .assertUpdates(false)
                .build();
    }
}