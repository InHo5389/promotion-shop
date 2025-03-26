package couponservice.outboxmessagerelay.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@EnableAsync      // 트랜잭션 끝나면은 카프카에 대한 이벤트 전송을 비동기로 처리
@Configuration
@EnableScheduling // 전송되지 않은 이벤트들을 주기적으로 가져와서 폴링해서 카프카로 보내기 위함
public class MessageRelayConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // KafkaTemplate - 카프카로 프로듀서 애플리케이션들이 이벤트를
    @Bean
    public KafkaTemplate<String, String> messageRelayKafkaTemplate() {
        // 카프카에 대한 프로듀서 설정
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(configProps));
    }

    // TODO 기능이 뭔지 찾아보기
    // 트랜잭션이 끝날 때마다 이벤트 전송을 기동기로 전송하기 위해서 그걸 처리하는 스레드풀
    @Bean
    public Executor messageRelayPublishEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("mr-pub-event-");
        return executor;
    }

    /**
     * 이벤트 전송이 아직 되지 않은 것들 그게 10초 이후에 이벤트들은 주기적으로 보내준다고 했었는데 그걸 위한 이제 스레드풀\
     * 어차피 Shard가 조금씩 분할되어서 할당될 거여가지고 싱글스레드로만 이제 미전송 이벤트들을 전송
     */
    @Bean
    public Executor messageRelayPublishPendingEventExecutor(){
        return Executors.newSingleThreadScheduledExecutor();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
