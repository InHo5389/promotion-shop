package pointbatchservice.step;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.BatchResult;
import org.redisson.api.RBatch;
import org.redisson.api.RMapAsync;
import org.redisson.api.RedissonClient;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import pointbatchservice.entity.PointBalance;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncPointBalance {

    private static final int CHUNK_SIZE = 5000;
    private static final int THREAD_COUNT = 8;

    private final JobRepository jobRepository;
    private final RedissonClient redissonClient;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;

    /**
     * 포인트 잔액 동기화 Step
     * <p>
     * DB의 포인트 잔액 정보를 Redis 캐시에 동기화
     * - Reader: JPA별 통계 포인트 잔액 조회
     * - Processor: 캐시 키 생성
     * - Writer: Redis에 포인트 잔액 저장
     */
    @Bean
    public Step syncPointBalanceStep() {
        return new StepBuilder("syncPointBalanceStep", jobRepository)
                .<PointBalance, Map.Entry<String, Long>>chunk(CHUNK_SIZE, transactionManager)
                .reader(pointSynchronizedItemReader())
                .processor(pointBalanceProcessor())
                .writer(pointBalanceWriter())
                .taskExecutor(syncPointBalanceTaskExecutor())
                .build();
    }

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<PointBalance> pointSynchronizedItemReader() {
        return new SynchronizedItemStreamReaderBuilder<PointBalance>()
                .delegate(pointBalanceReader())
                .build();
    }

    /**
     * 포인트 잔액 Reader
     * <p>
     * JPA를 사용하여 DB에서 포인트 잔액 정보를 조회
     */
    @Bean
    @StepScope
    public JdbcCursorItemReader<PointBalance> pointBalanceReader() {
        return new JdbcCursorItemReaderBuilder<PointBalance>()
                .name("pointBalanceReader")
                .dataSource(dataSource)
                .sql("SELECT id, user_id, balance, version, created_at, modified_at FROM point_balances")
                .fetchSize(CHUNK_SIZE)
                .verifyCursorPosition(false)
                .preparedStatementSetter(ps -> {
                    ps.setFetchSize(Integer.MIN_VALUE);
                })
                .rowMapper((rs, rowNum) -> mapResultSetToPointBalance(rs))
                .build();
    }

    private PointBalance mapResultSetToPointBalance(ResultSet rs) throws SQLException {
        return PointBalance.builder()
                .id(rs.getLong("id"))
                .userId(rs.getLong("user_id"))
                .balance(rs.getLong("balance"))
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<PointBalance, Map.Entry<String, Long>> pointBalanceProcessor() {
        return pointBalance -> Map.entry(
                String.format("point:balance:%d", pointBalance.getUserId()),
                pointBalance.getBalance()
        );
    }

    /**
     * Redis 캐시에 포인트 잔액 저장
     */
    @Bean
    @StepScope
    public ItemWriter<Map.Entry<String, Long>> pointBalanceWriter() {
        return items -> {
            RBatch batch = redissonClient.createBatch();
            RMapAsync<String, Long> asyncMap = batch.getMap("point:balance");

            items.forEach(item -> {
                asyncMap.putAsync(item.getKey(), item.getValue());
            });

            BatchResult<?> result = batch.execute();

            log.debug("Saved {} items to Redis in thread: {}",
                    items.size(), Thread.currentThread().getName());
        };
    }

    @Bean
    public TaskExecutor syncPointBalanceTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(THREAD_COUNT);
        executor.setMaxPoolSize(THREAD_COUNT);
        executor.setQueueCapacity(CHUNK_SIZE);
        executor.setThreadNamePrefix("sync-balance-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();

        log.info("Configured ThreadPoolTaskExecutor with {} threads for syncPointBalance", THREAD_COUNT);
        return executor;
    }
}
