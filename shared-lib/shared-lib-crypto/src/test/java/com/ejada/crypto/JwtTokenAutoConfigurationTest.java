package com.ejada.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = JwtTokenAutoConfiguration.class)
@TestPropertySource(properties = {
        "shared.security.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
        "shared.security.jwt.token-period=PT5M"
})
class JwtTokenAutoConfigurationTest {

    @Autowired
    private JwtTokenService jwtTokenService;

    @Test
    void beanLoadsAndGeneratesToken() {
        String token = jwtTokenService.generateToken("alice");
        byte[] keyBytes = Base64.getDecoder().decode("MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=");
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        assertEquals("alice", claims.getSubject());
        assertNotNull(claims.getExpiration());
    }
}
