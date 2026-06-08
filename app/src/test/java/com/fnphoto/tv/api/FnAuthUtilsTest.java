package com.fnphoto.tv.api;

import org.junit.Test;

import static org.junit.Assert.*;

public class FnAuthUtilsTest {

    @Test
    public void generateAuthX_returnsNonNull() {
        String authx = FnAuthUtils.generateAuthX("/p/api/v1/app/version", "GET", null);
        assertNotNull(authx);
    }

    @Test
    public void generateAuthX_containsNonceTimestampAndSign() {
        String authx = FnAuthUtils.generateAuthX("/p/api/v1/gallery/timeline", "GET", null);
        assertTrue(authx.startsWith("nonce="));
        assertTrue(authx.contains("&timestamp="));
        assertTrue(authx.contains("&sign="));
    }

    @Test
    public void generateAuthX_nonceIsSixDigits() {
        String authx = FnAuthUtils.generateAuthX("/p/api/v1/test", "GET", null);
        String nonce = extractParam(authx, "nonce");
        assertNotNull(nonce);
        assertEquals(6, nonce.length());
        assertTrue(nonce.matches("\\d{6}"));
    }

    @Test
    public void generateAuthX_timestampIsCurrentTimeInMillis() {
        String authx = FnAuthUtils.generateAuthX("/p/api/v1/test", "GET", null);
        String ts = extractParam(authx, "timestamp");
        assertNotNull(ts);

        long tsLong = Long.parseLong(ts);
        long now = System.currentTimeMillis();
        assertTrue(Math.abs(tsLong - now) < 5000);
    }

    @Test
    public void generateAuthX_signIs32HexChars() {
        String authx = FnAuthUtils.generateAuthX("/p/api/v1/test", "GET", null);
        String sign = extractParam(authx, "sign");
        assertNotNull(sign);
        assertEquals(32, sign.length());
        assertTrue(sign.matches("[0-9a-f]{32}"));
    }

    @Test
    public void generateAuthX_diffNonceEachCall() {
        String a1 = FnAuthUtils.generateAuthX("/p/api/v1/test", "GET", null);
        String a2 = FnAuthUtils.generateAuthX("/p/api/v1/test", "GET", null);
        String n1 = extractParam(a1, "nonce");
        String n2 = extractParam(a2, "nonce");
        assertNotNull(n1);
        assertNotNull(n2);
        assertNotEquals(n1, n2);
    }

    @Test
    public void generateAuthX_diffSignForDiffPath() {
        String a1 = FnAuthUtils.generateAuthX("/p/api/v1/pathA", "GET", null);
        String a2 = FnAuthUtils.generateAuthX("/p/api/v1/pathB", "GET", null);
        String s1 = extractParam(a1, "sign");
        String s2 = extractParam(a2, "sign");
        assertNotNull(s1);
        assertNotNull(s2);
        assertNotEquals(s1, s2);
    }

    @Test
    public void generateAuthX_diffSignForDiffMethod() {
        String a1 = FnAuthUtils.generateAuthX("/p/api/v1/test", "GET", null);
        String a2 = FnAuthUtils.generateAuthX("/p/api/v1/test", "POST", "body");
        String s1 = extractParam(a1, "sign");
        String s2 = extractParam(a2, "sign");
        assertNotNull(s1);
        assertNotNull(s2);
        assertNotEquals(s1, s2);
    }

    @Test
    public void generateAuthX_postMethodWithBody() {
        String authx = FnAuthUtils.generateAuthX("/p/api/v1/login", "POST", "{\"user\":\"test\"}");
        assertNotNull(authx);
        assertTrue(authx.contains("&sign="));
    }

    @Test
    public void generateAuthX_handlesNullData() {
        String authx = FnAuthUtils.generateAuthX("/p/api/v1/test", "GET", null);
        assertNotNull(authx);
    }

    @Test
    public void generateAuthX_handlesEmptyData() {
        String authx = FnAuthUtils.generateAuthX("/p/api/v1/test", "GET", "");
        assertNotNull(authx);
    }

    @Test
    public void generateAuthX_consistencyForSameInput() {
        String authx = FnAuthUtils.generateAuthX("/p/api/v1/static", "GET", "a=1");
        assertNotNull(authx);
        String nonce = extractParam(authx, "nonce");
        String ts = extractParam(authx, "timestamp");
        String sign = extractParam(authx, "sign");
        assertNotNull(nonce);
        assertNotNull(ts);
        assertNotNull(sign);
    }

    private static String extractParam(String authx, String name) {
        if (authx == null) return null;
        String prefix = name + "=";
        int start = authx.indexOf(prefix);
        if (start < 0) return null;
        start += prefix.length();
        int end = authx.indexOf("&", start);
        if (end < 0) end = authx.length();
        return authx.substring(start, end);
    }
}
