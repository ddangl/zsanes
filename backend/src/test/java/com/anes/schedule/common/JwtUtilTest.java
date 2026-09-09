package com.anes.schedule.common;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** JWT 签发/解析/过期/伪造 单元测试 */
class JwtUtilTest {

    private static final String SECRET = "test-secret-key-0123456789-0123456789-0123456789";

    private final JwtUtil jwtUtil = new JwtUtil(SECRET, 12);

    @Test
    void issueAndParseRoundTrip() {
        String token = jwtUtil.issue(7L, "admin", "ADMIN");
        Claims claims = jwtUtil.parse(token);
        assertNotNull(claims);
        assertEquals("7", claims.getSubject());
        assertEquals("admin", claims.get("username"));
        assertEquals("ADMIN", claims.get("role"));
    }

    @Test
    void expiredTokenReturnsNull() {
        JwtUtil expired = new JwtUtil(SECRET, -1);
        assertNull(expired.parse(expired.issue(1L, "a", "USER")));
    }

    @Test
    void malformedOrWrongKeyTokenReturnsNull() {
        assertNull(jwtUtil.parse("not-a-jwt"));
        JwtUtil otherKey = new JwtUtil(SECRET + "different", 12);
        assertNull(otherKey.parse(jwtUtil.issue(1L, "a", "USER")));
    }
}
