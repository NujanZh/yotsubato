package io.github.nujanzh.yotsubato;

import io.github.nujanzh.yotsubato.security.jwt.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class YotsubatoApplication {

    public static void main(String[] args) {
        SpringApplication.run(YotsubatoApplication.class, args);
    }
}
