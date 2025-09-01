package com.shared.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = JwtTokenAutoConfiguration.class)
@TestPropertySource(properties = "shared.security.jwt.secret=01234567890123456789012345678901")
class JwtTokenAutoConfigurationTest {

    @Autowired
    private JwtTokenService jwtTokenService;

    @Test
    void beanLoadsAndGeneratesToken() {
        String token = jwtTokenService.generateToken("alice");
        SecretKey key = Keys.hmacShaKeyFor("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        assertEquals("alice", claims.getSubject());
    }
}
