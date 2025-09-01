package com.shared.crypto;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTest {

    @Test
    void createTokenIncludesTenantAndRoles() {
        SecretKey key = Keys.hmacShaKeyFor("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));
        JwtTokenService service = new JwtTokenService(key);
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
