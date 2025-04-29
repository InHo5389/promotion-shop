package pointservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaAuditing
@EnableDiscoveryClient
@ComponentScan(basePackages = {"pointservice", "outboxmessagerelay"})
@EntityScan(basePackages = {"pointservice.entity", "outboxmessagerelay.entity"})
@EnableJpaRepositories(basePackages = {"pointservice.repository", "outboxmessagerelay.repository"})
@SpringBootApplication
public class PointServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PointServiceApplication.class, args);
	}
}
