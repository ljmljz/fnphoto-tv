package com.fnphoto.tv;

import com.fnphoto.tv.api.UrlUtils;

import org.junit.Test;

import static org.junit.Assert.*;

public class LoginActivityTest {

    @Test
    public void normalizeUrl_plainIpAddsDefaultPort() {
        assertEquals("http://192.168.1.100:5666", UrlUtils.normalizeUrl("192.168.1.100"));
    }

    @Test
    public void normalizeUrl_ipWithPort() {
        assertEquals("http://192.168.1.100:5666", UrlUtils.normalizeUrl("192.168.1.100:5666"));
    }

    @Test
    public void normalizeUrl_httpUrlWithoutPortAddsDefault() {
        assertEquals("http://192.168.1.100:5666", UrlUtils.normalizeUrl("http://192.168.1.100"));
    }

    @Test
    public void normalizeUrl_httpUrlWithPort() {
        assertEquals("http://192.168.1.100:5666", UrlUtils.normalizeUrl("http://192.168.1.100:5666"));
    }

    @Test
    public void normalizeUrl_httpsUrlWithoutPortAddsDefault() {
        assertEquals("https://192.168.1.100:5666", UrlUtils.normalizeUrl("https://192.168.1.100"));
    }

    @Test
    public void normalizeUrl_httpsUrlWithPort() {
        assertEquals("https://192.168.1.100:443", UrlUtils.normalizeUrl("https://192.168.1.100:443"));
    }

    @Test
    public void normalizeUrl_domainName() {
        assertEquals("http://mynas.local:5666", UrlUtils.normalizeUrl("mynas.local"));
    }

    @Test
    public void normalizeUrl_domainWithPort() {
        assertEquals("http://mynas.local:8080", UrlUtils.normalizeUrl("mynas.local:8080"));
    }

    @Test
    public void normalizeUrl_httpDomain() {
        assertEquals("http://photos.example.com:5666", UrlUtils.normalizeUrl("http://photos.example.com"));
    }

    @Test
    public void normalizeUrl_httpsDomain() {
        assertEquals("https://photos.example.com:5666", UrlUtils.normalizeUrl("https://photos.example.com"));
    }

    @Test
    public void normalizeUrl_ipv6Bracketed() {
        assertEquals("http://[::1]:5666", UrlUtils.normalizeUrl("[::1]"));
    }

    @Test
    public void normalizeUrl_ipv6BracketedWithPort() {
        assertEquals("http://[::1]:5666", UrlUtils.normalizeUrl("[::1]:5666"));
    }

    @Test
    public void normalizeUrl_httpIpv6() {
        assertEquals("http://[::1]:5666", UrlUtils.normalizeUrl("http://[::1]"));
    }

    @Test
    public void normalizeUrl_httpsIpv6() {
        assertEquals("https://[::1]:5666", UrlUtils.normalizeUrl("https://[::1]"));
    }

    @Test
    public void normalizeUrl_ipv6httpWithPort() {
        assertEquals("http://[::1]:5666", UrlUtils.normalizeUrl("http://[::1]:5666"));
    }

    @Test
    public void normalizeUrl_fnId() {
        assertEquals("http://mynas-1234:5666", UrlUtils.normalizeUrl("mynas-1234"));
    }

    @Test
    public void normalizeUrl_pathDoesNotAffectHost() {
        assertEquals("http://192.168.1.100:5666", UrlUtils.normalizeUrl("http://192.168.1.100/some/path"));
    }

    @Test
    public void normalizeUrl_keepsHttpSchemeForHttpInput() {
        String result = UrlUtils.normalizeUrl("http://nas.local");
        assertTrue(result.startsWith("http://"));
        assertFalse(result.startsWith("https://"));
    }

    @Test
    public void normalizeUrl_keepsHttpsSchemeForHttpsInput() {
        String result = UrlUtils.normalizeUrl("https://nas.local");
        assertTrue(result.startsWith("https://"));
    }

    @Test
    public void normalizeUrl_addsHttpByDefault() {
        String result = UrlUtils.normalizeUrl("nas.local");
        assertTrue(result.startsWith("http://"));
    }

    @Test
    public void normalizeUrl_ipWithLeadingZeros() {
        assertEquals("http://10.0.0.1:5666", UrlUtils.normalizeUrl("10.0.0.1"));
    }

    @Test
    public void normalizeUrl_ipv4WithHttpsAndPort() {
        assertEquals("https://10.0.0.1:8443", UrlUtils.normalizeUrl("https://10.0.0.1:8443"));
    }

    @Test
    public void normalizeUrl_ipv6BracketedWithCustomPort() {
        assertEquals("http://[::1]:8080", UrlUtils.normalizeUrl("[::1]:8080"));
    }

    @Test
    public void normalizeUrl_httpsIpv6WithCustomPort() {
        assertEquals("https://[::1]:8443", UrlUtils.normalizeUrl("https://[::1]:8443"));
    }
}
