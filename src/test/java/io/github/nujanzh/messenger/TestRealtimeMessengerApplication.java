package io.github.nujanzh.messenger;

import org.springframework.boot.SpringApplication;

public class TestRealtimeMessengerApplication {

	public static void main(String[] args) {
		SpringApplication.from(RealtimeMessengerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
