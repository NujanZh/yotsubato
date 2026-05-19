package io.github.nujanzh.yotsubato.security;

import io.github.nujanzh.yotsubato.config.JwtProperties;
import io.github.nujanzh.yotsubato.exception.JwtValidationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
public class JwtService {

    private static final SecureRandom RNG = new SecureRandom();
    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final String issuer;
    private final Duration accessTokenTtl;

    public JwtService(JwtProperties props) {
        try {
            this.publicKey = readX509PublicKey(props.publicKey());
            this.privateKey = readPKCS8PrivateKey(props.privateKey());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load JWT keys", e);
        }
        this.issuer = props.issuer();
        this.accessTokenTtl = props.accessTokenTtl();
    }

    private RSAPublicKey readX509PublicKey(Resource publicKeyResource) throws IOException {
        return RsaKeyConverters.x509().convert(publicKeyResource.getInputStream());
    }

    private RSAPrivateKey readPKCS8PrivateKey(Resource privateKeyResource) throws IOException {
        return RsaKeyConverters.pkcs8().convert(privateKeyResource.getInputStream());
    }

    public String generateAccessToken(UserDetails userDetails) {
        Instant now = Instant.now();
        return Jwts.builder()
                // subject = user UUID per CustomUserDetails contract
                .subject(userDetails.getUsername())
                .issuer(this.issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(this.accessTokenTtl)))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public String generateRefreshToken() {
        byte[] bytes = new byte[32];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public AuthenticatedPrincipal parseAndValidate(String token) {
        try {
            Jws<Claims> claimsJws =
                    Jwts.parser()
                            .verifyWith(publicKey)
                            .requireIssuer(this.issuer)
                            .clockSkewSeconds(30)
                            .build()
                            .parseSignedClaims(token);

            UUID userId = parseSubject(claimsJws.getPayload());
            // TODO: JTI for blocklist
            // String tokenId = claimsJws.getPayload().getId();
            Instant expiresAt = claimsJws.getPayload().getExpiration().toInstant();

            return new AuthenticatedPrincipal(userId, expiresAt);
        } catch (ExpiredJwtException ex) {
            throw new JwtValidationException("Token expired", ex);
        } catch (SignatureException ex) {
            throw new JwtValidationException("Invalid signature", ex);
        } catch (UnsupportedJwtException ex) {
            throw new JwtValidationException("Unsupported token", ex);
        } catch (MalformedJwtException ex) {
            throw new JwtValidationException("Malformed token", ex);
        } catch (IncorrectClaimException ex) {
            throw new JwtValidationException("Wrong issuer", ex);
        } catch (JwtException ex) {
            throw new JwtValidationException("Invalid token", ex);
        } catch (IllegalArgumentException ex) {
            throw new JwtValidationException("Empty token", ex);
        }
    }

    private UUID parseSubject(Claims payload) {
        String subject = payload.getSubject();

        if (subject == null) {
            throw new JwtValidationException("Missing subject claim");
        }

        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            throw new JwtValidationException("Invalid subject claim", e);
        }
    }
}
