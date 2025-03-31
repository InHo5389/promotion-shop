package pointbatchservice.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pointbatchservice.listener.JobCompletionNotificationListener;
import pointbatchservice.step.SyncPointBalance;
import pointbatchservice.step.v4.GenerateDailyReportV4;

/**
 * 포인트 잔액 동기화 및 일별 리포트 생성을 위한 배치 Job 설정
 * <p>
 * 주요 기능:
 * 1. Redis 캐시와 DB의 포인트 잔액 동기화
 * 2. 전일 포인트 트랜잭션 기반 일별 리포트 생성
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class PointBalanceSyncJobConfig {

    private final JobRepository jobRepository;

    private final SyncPointBalance syncPointBalance;
    private final GenerateDailyReportV4 generateDailyReportV4;
    private final JobCompletionNotificationListener jobCompletionNotificationListener;

    /**
     * 포인트 잔액 동기화 및 일별 포인트 생성 Job
     * <p>
     * 1. syncPointBalanceStep : DB의 포인트 잔액을 Redis 캐시에 동기화
     * 2. generateDailyReportStep: 전일 포인트 트랜잭션을 집계 후 일별 리포트 생성
     */
    @Bean
    public Job pointBalanceSyncJob() {
        return new JobBuilder("pointBalanceSyncJob", jobRepository)
                .listener(jobCompletionNotificationListener)
                .start(syncPointBalance.syncPointBalanceStep())
                .next(generateDailyReportV4.generateDailyReportStepV4())
                .build();
    }
}
