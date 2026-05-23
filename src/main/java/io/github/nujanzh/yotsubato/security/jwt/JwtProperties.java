package io.github.nujanzh.yotsubato.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.time.Duration;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        Resource privateKey,
        Resource publicKey,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        String issuer) {}
