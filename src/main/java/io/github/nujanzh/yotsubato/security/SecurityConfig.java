package io.github.nujanzh.yotsubato.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ProblemDetailAuthEntryPoint problemDetailAuthEntryPoint;
    private final ProblemDetailAccessDeniedHandler problemDetailAccessDeniedHandler;

    public SecurityConfig(
            JwtAuthFilter jwtAuthFilter,
            ProblemDetailAuthEntryPoint problemDetailAuthEntryPoint,
            ProblemDetailAccessDeniedHandler problemDetailAccessDeniedHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.problemDetailAuthEntryPoint = problemDetailAuthEntryPoint;
        this.problemDetailAccessDeniedHandler = problemDetailAccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/auth/**",
                                                "/swagger-ui/**",
                                                "/api-docs/**",
                                                "/actuator/health")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(
                        ex ->
                                ex.authenticationEntryPoint(problemDetailAuthEntryPoint)
                                        .accessDeniedHandler(problemDetailAccessDeniedHandler))
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
