package pointbatchservice;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

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
			jobLauncher.run(
					pointBalanceSyncJob,
					new JobParametersBuilder()
							.addLong("timestamp",System.currentTimeMillis())
							.toJobParameters()
			);
		};
	}
}
