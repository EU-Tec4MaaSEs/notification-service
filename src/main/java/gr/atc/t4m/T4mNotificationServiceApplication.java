package gr.atc.t4m;

import gr.atc.t4m.config.properties.KafkaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableConfigurationProperties(KafkaProperties.class)
@EnableAsync
public class T4mNotificationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(T4mNotificationServiceApplication.class, args);
	}

}
