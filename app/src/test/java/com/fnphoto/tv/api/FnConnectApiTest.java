package com.fnphoto.tv.api;

import org.junit.Test;

import static org.junit.Assert.*;

public class FnConnectApiTest {

    @Test
    public void isFnId_returnsTrueForValidId() {
        assertTrue(FnConnectApi.isFnId("mynas-1234"));
    }

    @Test
    public void isFnId_returnsTrueForMinLengthId() {
        assertTrue(FnConnectApi.isFnId("abcde"));
    }

    @Test
    public void isFnId_returnsTrueForMaxLengthId() {
        assertTrue(FnConnectApi.isFnId("abcdefghijklmnopqrstuvwxyz012345"));
    }

    @Test
    public void isFnId_returnsFalseForTooShortId() {
        assertFalse(FnConnectApi.isFnId("abcd"));
    }

    @Test
    public void isFnId_returnsFalseForEmptyInput() {
        assertFalse(FnConnectApi.isFnId(""));
    }

    @Test
    public void isFnId_returnsFalseForNullInput() {
        assertFalse(FnConnectApi.isFnId(null));
    }

    @Test
    public void isFnId_returnsFalseForIpAddress() {
        assertFalse(FnConnectApi.isFnId("192.168.1.100"));
    }

    @Test
    public void isFnId_returnsFalseForUrlWithColon() {
        assertFalse(FnConnectApi.isFnId("192.168.1.100:5666"));
    }

    @Test
    public void isFnId_returnsFalseForUrlWithSlash() {
        assertFalse(FnConnectApi.isFnId("http://example.com"));
    }

    @Test
    public void isFnId_returnsFalseForLocalhost() {
        assertFalse(FnConnectApi.isFnId("localhost"));
    }

    @Test
    public void isFnId_returnsFalseForLocalhostCaseInsensitive() {
        assertFalse(FnConnectApi.isFnId("LocalHost"));
    }

    @Test
    public void isFnId_returnsFalseForIdEndingWithDash() {
        assertFalse(FnConnectApi.isFnId("mynas-"));
    }

    @Test
    public void isFnId_returnsFalseForIdStartingWithNumber() {
        assertFalse(FnConnectApi.isFnId("1mynas"));
    }

    @Test
    public void isFnId_acceptsAlphanumericAndDash() {
        assertTrue(FnConnectApi.isFnId("my-nas-001"));
    }

    @Test
    public void nasAddr_ipv4HttpUrl() {
        FnConnectApi.NasAddr addr = new FnConnectApi.NasAddr("192.168.1.100", "ipv4", 5666);
        assertEquals("http://192.168.1.100:5666", addr.toHttpUrl());
    }

    @Test
    public void nasAddr_ipv4WithCustomPort() {
        FnConnectApi.NasAddr addr = new FnConnectApi.NasAddr("192.168.1.100:5666", "ipv4", 5666);
        assertEquals("http://192.168.1.100:5666", addr.toHttpUrl());
    }

    @Test
    public void nasAddr_ipv6HttpUrl() {
        FnConnectApi.NasAddr addr = new FnConnectApi.NasAddr("::1", "ipv6", 5666);
        assertEquals("http://[::1]:5666", addr.toHttpUrl());
    }

    @Test
    public void nasAddr_ipv6WithPort() {
        FnConnectApi.NasAddr addr = new FnConnectApi.NasAddr("::1", "ipv6", 5666);
        assertEquals("http://[::1]:5666", addr.toHttpUrl());
    }

    @Test
    public void nasAddr_wsUrl() {
        FnConnectApi.NasAddr addr = new FnConnectApi.NasAddr("192.168.1.100", "ipv4", 5666);
        assertEquals("ws://192.168.1.100:5666/websocket?type=main", addr.toWsUrl());
    }

    @Test
    public void nasAddr_ipv6WsUrl() {
        FnConnectApi.NasAddr addr = new FnConnectApi.NasAddr("::1", "ipv6", 5666);
        assertEquals("ws://[::1]:5666/websocket?type=main", addr.toWsUrl());
    }

    @Test
    public void nasAddr_parsesPortFromAddress() {
        FnConnectApi.NasAddr addr = new FnConnectApi.NasAddr("mynas.example.com:443", "ddns", 5666);
        assertEquals("mynas.example.com", addr.address);
        assertEquals(443, addr.port);
    }

    @Test
    public void nasAddr_usesDefaultPortWhenNoColon() {
        FnConnectApi.NasAddr addr = new FnConnectApi.NasAddr("mynas.example.com", "ddns", 5666);
        assertEquals("mynas.example.com", addr.address);
        assertEquals(5666, addr.port);
    }

    @Test
    public void nasAddr_ignoresPortForIpv6() {
        FnConnectApi.NasAddr addr = new FnConnectApi.NasAddr("::1", "ipv6", 5666);
        assertEquals("::1", addr.address);
        assertEquals(5666, addr.port);
    }

    @Test
    public void nasAddr_keepsOriginalAddressWhenPortNotNumeric() {
        FnConnectApi.NasAddr addr = new FnConnectApi.NasAddr("host:port", "ipv4", 5666);
        assertEquals("host:port", addr.address);
        assertEquals(5666, addr.port);
    }
}
