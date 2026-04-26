package com.campus.education.common;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilsTest {

    @Test
    void shouldRejectShortJwtSecret() {
        JwtUtils jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secret", "short-secret");
        ReflectionTestUtils.setField(jwtUtils, "expiration", 7200000L);

        assertThrows(IllegalStateException.class, jwtUtils::init);
    }

    @Test
    void shouldGenerateAndParseTokenWithStrongSecret() {
        JwtUtils jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secret", "12345678901234567890123456789012");
        ReflectionTestUtils.setField(jwtUtils, "expiration", 7200000L);
        jwtUtils.init();

        String token = jwtUtils.generateToken("U001", "student1", "5");

        assertTrue(jwtUtils.validateToken(token));
        assertEquals("U001", jwtUtils.getUserIdFromToken(token));
        assertEquals("student1", jwtUtils.getUsernameFromToken(token));
        assertEquals("5", jwtUtils.getRoleIdFromToken(token));
    }
}
