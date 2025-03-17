package pointbatchservice.job;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import pointbatchservice.repository.DailyPointReportJpaRepository;
import pointbatchservice.repository.PointBalanceJpaRepository;
import pointbatchservice.repository.PointJpaRepository;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
class PointBalanceSyncJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @MockBean
    private PointJpaRepository pointJpaRepository;

    @MockBean
    private PointBalanceJpaRepository pointBalanceJpaRepository;

    @Autowired
    private DailyPointReportJpaRepository dailyPointReportJpaRepository;

    @MockBean
    private RedissonClient redissonClient;

    @MockBean
    private RMap<String, Long> balanceMap;

    @BeforeEach
    void setUp() {
        when(redissonClient.<String, Long>getMap(anyString())).thenReturn(balanceMap);

        dailyPointReportJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("포인트 동기화 Job 실행 성공 테스트")
    void jobExecutionTest() throws Exception {
        //given
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("dateTime", LocalDateTime.now().toString())
                .toJobParameters();
        //when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        //then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    }

    @Test
    @DisplayName("Redis 캐시 동기화 Step 테스트")
    void syncPointBalanceStepTest() {
        //given
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("dateTime", LocalDateTime.now().toString())
                .toJobParameters();

        //when
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("syncPointBalanceStep", jobParameters);
        //then
        assertThat(jobExecution.getStepExecutions()).hasSize(1);
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    @DisplayName("일별 리포트 생성 Step 테스트")
    void generateDailyReportStepTest() {
        //given
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("dateTime", LocalDateTime.now().toString())
                .toJobParameters();

        //when
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("generateDailyReportStep", jobParameters);
        //then
        assertThat(jobExecution.getStepExecutions()).hasSize(1);
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }
}