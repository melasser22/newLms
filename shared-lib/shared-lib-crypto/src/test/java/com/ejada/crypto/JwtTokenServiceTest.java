package com.ejada.crypto;

import com.ejada.common.constants.JwtClaims;
import io.jsonwebtoken.Jwts;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTest {

    @Test
    void createTokenIncludesTenantAndRoles() {
        SecretKey key = JwtTestFixtures.signingKey();
        JwtTokenService service = JwtTokenService.withSecret(JwtTestFixtures.TEST_SECRET_B64, null);
        Map<String, Object> claims = Map.of("custom", "value");
        List<String> roles = List.of("admin", "user");
        String token = service.createToken("user", "tenant1", roles, claims, Duration.ofMinutes(5));
        assertNotNull(token);

        var parsed = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
        assertEquals("user", parsed.getPayload().getSubject());
        assertEquals("tenant1", parsed.getPayload().get(JwtClaims.TENANT));
        assertEquals(roles, parsed.getPayload().get(JwtClaims.ROLES, List.class));
        assertEquals("value", parsed.getPayload().get("custom"));
    }
}
