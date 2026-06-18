package io.github.nujanzh.yotsubato.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class RequestMatcherConfig {

    @Bean
    public RequestMatcher publicEndPoints() {
        PathPatternRequestMatcher.Builder factory = PathPatternRequestMatcher.withDefaults();

        return new OrRequestMatcher(
                factory.matcher("/auth/**"),
                factory.matcher("/swagger-ui/**"),
                factory.matcher("/api-docs/**"),
                factory.matcher("/actuator/health/**"),
                factory.matcher("/ws/**"));
    }
}
