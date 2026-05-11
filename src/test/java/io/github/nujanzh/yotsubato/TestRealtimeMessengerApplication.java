package io.github.nujanzh.yotsubato;

import org.springframework.boot.SpringApplication;

public class TestRealtimeMessengerApplication {

    public static void main(String[] args) {
        SpringApplication.from(YotsubatoApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
