package gr.atc.t4m;

import org.springframework.boot.SpringApplication;

public class TestT4mNotificationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(TestT4mNotificationServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
