package couponservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@EnableDiscoveryClient
@EnableScheduling
@ComponentScan(basePackages = {"couponservice", "outboxmessagerelay"})
@EntityScan(basePackages = {"couponservice.entity", "outboxmessagerelay.entity"})
@EnableJpaRepositories(basePackages = {"couponservice.repository", "outboxmessagerelay.repository"})
@SpringBootApplication
public class CouponServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CouponServiceApplication.class, args);
	}

}
