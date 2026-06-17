package io.github.nujanzh.yotsubato.security.jwt;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        Resource privateKey,
        Resource publicKey,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        String issuer) {}
