package com.shared.crypto;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTest {

    @Test
    void createTokenIncludesTenantAndRoles() {
        String secret = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        JwtTokenService service = JwtTokenService.withSecret(secret, null);
        Map<String, Object> claims = Map.of("custom", "value");
        List<String> roles = List.of("admin", "user");
        String token = service.createToken("user", "tenant1", roles, claims, Duration.ofMinutes(5));
        assertNotNull(token);

        var parsed = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
        assertEquals("user", parsed.getPayload().getSubject());
        assertEquals("tenant1", parsed.getPayload().get("tenant"));
        assertEquals(roles, parsed.getPayload().get("roles", List.class));
        assertEquals("value", parsed.getPayload().get("custom"));
    }
}
