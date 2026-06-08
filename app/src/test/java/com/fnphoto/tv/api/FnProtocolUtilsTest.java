package com.fnphoto.tv.api;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class FnProtocolUtilsTest {

    @Before
    public void setUp() {
        FnProtocolUtils.setBackId("0000000000000000");
    }

    @After
    public void tearDown() {
        FnProtocolUtils.setBackId("0000000000000000");
    }

    @Test
    public void generateReqId_returns28CharHex() {
        String reqId = FnProtocolUtils.generateReqId();
        assertNotNull(reqId);
        assertEquals(28, reqId.length());
        assertTrue(reqId.matches("[0-9a-f]{28}"));
    }

    @Test
    public void generateReqId_hasTimestampPrefix() {
        long before = System.currentTimeMillis() / 1000;
        String reqId = FnProtocolUtils.generateReqId();
        long after = System.currentTimeMillis() / 1000;
        String tsHex = reqId.substring(0, 8);
        long ts = Long.parseLong(tsHex, 16);
        assertTrue(ts >= before && ts <= after);
    }

    @Test
    public void generateReqId_hasZerosInMiddle() {
        String reqId = FnProtocolUtils.generateReqId();
        assertEquals("0000000000000000", reqId.substring(8, 24));
    }

    @Test
    public void generateReqId_incrementsIndex() {
        String r1 = FnProtocolUtils.generateReqId();
        String r2 = FnProtocolUtils.generateReqId();
        String idx1 = r1.substring(24);
        String idx2 = r2.substring(24);
        int i1 = Integer.parseInt(idx1, 16);
        int i2 = Integer.parseInt(idx2, 16);
        assertEquals(i1 + 1, i2);
    }

    @Test
    public void generateReqIdWithBackId_usesProvidedBackId() {
        String backId = "1234567890abcdef";
        String reqId = FnProtocolUtils.generateReqIdWithBackId(backId);
        assertEquals(28, reqId.length());
        assertEquals(backId, reqId.substring(8, 24));
    }

    @Test
    public void generateReqIdWithBackId_fallsBackToZerosForNull() {
        String reqId = FnProtocolUtils.generateReqIdWithBackId(null);
        assertEquals("0000000000000000", reqId.substring(8, 24));
    }

    @Test
    public void generateReqIdWithBackId_fallsBackToZerosForShortBackId() {
        String reqId = FnProtocolUtils.generateReqIdWithBackId("short");
        assertEquals("0000000000000000", reqId.substring(8, 24));
    }

    @Test
    public void generateReqIdWithBackId_fallsBackToZerosForLongBackId() {
        String reqId = FnProtocolUtils.generateReqIdWithBackId("1234567890abcdefg");
        assertEquals("0000000000000000", reqId.substring(8, 24));
    }

    @Test
    public void setBackId_storesValidBackId() {
        FnProtocolUtils.setBackId("abcdef1234567890");
        assertEquals("abcdef1234567890", FnProtocolUtils.getBackId());
    }

    @Test
    public void setBackId_ignoresInvalidBackId() {
        FnProtocolUtils.setBackId("0000000000000000");
        FnProtocolUtils.setBackId("too-short");
        assertEquals("0000000000000000", FnProtocolUtils.getBackId());

        FnProtocolUtils.setBackId("this-is-way-too-long-id");
        assertEquals("0000000000000000", FnProtocolUtils.getBackId());
    }

    @Test
    public void generateRandomString_returnsExactLength() {
        assertEquals(0, FnProtocolUtils.generateRandomString(0).length());
        assertEquals(1, FnProtocolUtils.generateRandomString(1).length());
        assertEquals(16, FnProtocolUtils.generateRandomString(16).length());
        assertEquals(32, FnProtocolUtils.generateRandomString(32).length());
        assertEquals(64, FnProtocolUtils.generateRandomString(64).length());
    }

    @Test
    public void generateRandomString_containsOnlyValidChars() {
        String result = FnProtocolUtils.generateRandomString(1000);
        assertTrue(result.matches("[0-9a-zA-Z]+"));
    }

    @Test
    public void generateRandomString_producesDifferentResults() {
        String s1 = FnProtocolUtils.generateRandomString(32);
        String s2 = FnProtocolUtils.generateRandomString(32);
        assertNotEquals(s1, s2);
    }

    @Test
    public void generateIV_returns16Bytes() {
        byte[] iv = FnProtocolUtils.generateIV();
        assertNotNull(iv);
        assertEquals(16, iv.length);
    }

    @Test
    public void generateIV_producesDifferentResults() {
        byte[] iv1 = FnProtocolUtils.generateIV();
        byte[] iv2 = FnProtocolUtils.generateIV();
        boolean same = true;
        for (int i = 0; i < 16; i++) {
            if (iv1[i] != iv2[i]) {
                same = false;
                break;
            }
        }
        assertFalse("IVs should differ", same);
    }

    @Test(expected = IllegalArgumentException.class)
    public void aesEncrypt_rejectsShortKey() throws Exception {
        FnProtocolUtils.aesEncrypt("data", "short", new byte[16]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void aesEncrypt_rejectsLongKey() throws Exception {
        FnProtocolUtils.aesEncrypt("data", "this-key-is-way-too-long-for-aes-256", new byte[16]);
    }

    @Test
    public void aesEncrypt_returnsBase64String() throws Exception {
        String key = "12345678901234567890123456789012";
        byte[] iv = new byte[16];
        String result = FnProtocolUtils.aesEncrypt("hello", key, iv);
        assertNotNull(result);
        assertTrue(result.length() > 0);
        assertTrue(android.util.Base64.decode(result, android.util.Base64.DEFAULT).length > 0);
    }

    @Test
    public void aesEncrypt_differentDataProducesDifferentResult() throws Exception {
        String key = "12345678901234567890123456789012";
        byte[] iv = new byte[16];
        String r1 = FnProtocolUtils.aesEncrypt("data1", key, iv);
        String r2 = FnProtocolUtils.aesEncrypt("data2", key, iv);
        assertNotEquals(r1, r2);
    }

    @Test
    public void aesEncrypt_differentKeyProducesDifferentResult() throws Exception {
        byte[] iv = new byte[16];
        String r1 = FnProtocolUtils.aesEncrypt("data", "11111111111111111111111111111111", iv);
        String r2 = FnProtocolUtils.aesEncrypt("data", "22222222222222222222222222222222", iv);
        assertNotEquals(r1, r2);
    }

    @Test
    public void aesEncrypt_withEmptyData() throws Exception {
        String key = "12345678901234567890123456789012";
        byte[] iv = new byte[16];
        String result = FnProtocolUtils.aesEncrypt("", key, iv);
        assertNotNull(result);
    }

    @Test
    public void rsaEncrypt_throwsWithoutValidKey() {
        try {
            FnProtocolUtils.rsaEncrypt("not-a-valid-key", "test");
            fail("Should have thrown exception");
        } catch (Exception e) {
            assertTrue(e instanceof java.security.GeneralSecurityException ||
                       e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void toCompactJson_producesNoWhitespace() {
        org.json.JSONObject json = new org.json.JSONObject();
        try {
            json.put("a", 1);
            json.put("b", "hello");
        } catch (Exception e) {
            fail("JSON exception: " + e.getMessage());
        }
        String result = FnProtocolUtils.toCompactJson(json);
        assertNotNull(result);
        assertFalse(result.contains(" "));
        assertFalse(result.contains("\n"));
    }
}
