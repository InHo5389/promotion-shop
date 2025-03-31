package pointbatchservice.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// TODO - 슬랙 연동
@Slf4j
@Component
public class JobCompletionNotificationListener implements JobExecutionListener {

    private ThreadLocal<Long> startTime = new ThreadLocal<>();

    @Override
    public void beforeJob(JobExecution jobExecution) {
        startTime.set(System.currentTimeMillis());
        log.info("Job {} is starting at {}...",
                jobExecution.getJobInstance().getJobName(),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime.get();

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("Job {} completed successfully at {}. Total execution time: {} seconds ({} ms)",
                    jobExecution.getJobInstance().getJobName(),
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    executionTime / 1000.0,
                    executionTime);
        } else {
            log.error("Job {} failed with status {} at {}. Total execution time: {} seconds ({} ms)",
                    jobExecution.getJobInstance().getJobName(),
                    jobExecution.getStatus(),
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    executionTime / 1000.0,
                    executionTime);
        }

        startTime.remove();
    }
}
