package pointbatchservice;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SpringBootApplication
@RequiredArgsConstructor
public class PointBatchServiceApplication {

	private final JobLauncher jobLauncher;
	private final Job pointBalanceSyncJob;

	public static void main(String[] args) {
		SpringApplication.run(PointBatchServiceApplication.class, args);
	}

	@Bean
	public ApplicationRunner runner(){
		return args -> {
			// 커맨드라인에서 targetDate 추출
			String targetDate = getTargetDateFromArgs(args);

			jobLauncher.run(
					pointBalanceSyncJob,
					new JobParametersBuilder()
							.addString("targetDate", targetDate)  // 추가!
							.addLong("timestamp", System.currentTimeMillis())
							.toJobParameters()
			);
		};
		// Spring Batch가 내부적으로 JobExecution을 생성함
	}

	private String getTargetDateFromArgs(ApplicationArguments args) {
		// --targetDate=2025-10-13 형태로 받기
		// 젠킨스에서 받은 날짜
		if (args.containsOption("targetDate")) {
			return args.getOptionValues("targetDate").get(0);
		}
		// 없으면 기본값 (전날)
		return LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ISO_DATE);
	}
}
