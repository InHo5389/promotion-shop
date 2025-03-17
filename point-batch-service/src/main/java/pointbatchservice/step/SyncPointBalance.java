package pointbatchservice.step;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
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
import pointbatchservice.entity.PointBalance;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncPointBalance {

    private static final int CHUNK_SIZE = 1000;

    private final JobRepository jobRepository;
    private final RedissonClient redissonClient;
    private final EntityManagerFactory entityManagerFactory;
    private final PlatformTransactionManager transactionManager;

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
                .reader(pointBalanceReader())
                .processor(pointBalanceProcessor())
                .writer(pointBalanceWriter())
                .build();
    }

    /**
     * 포인트 잔액 Reader
     * <p>
     * JPA를 사용하여 DB에서 포인트 잔액 정보를 조회
     */
    @Bean
    @StepScope
    public JpaPagingItemReader<PointBalance> pointBalanceReader() {
        return new JpaPagingItemReaderBuilder<PointBalance>()
                .name("pointBalanceReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryString("SELECT pb From PointBalance pb")
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
            var balanceMap = redissonClient.getMap("point:balance");
            items.forEach(item -> balanceMap.put(item.getKey(), item.getValue()));
        };
    }
}
