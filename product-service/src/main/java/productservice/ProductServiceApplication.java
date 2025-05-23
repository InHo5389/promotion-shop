package productservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaAuditing
@EnableDiscoveryClient
@ComponentScan(basePackages = {"productservice", "outboxmessagerelay"})
@EntityScan(basePackages = {"productservice.entity", "outboxmessagerelay.entity"})
@EnableJpaRepositories(basePackages = {"productservice.repository", "outboxmessagerelay.repository"})
@SpringBootApplication
public class ProductServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductServiceApplication.class, args);
	}

}
