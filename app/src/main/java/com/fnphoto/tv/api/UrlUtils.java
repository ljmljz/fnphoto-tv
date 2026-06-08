package com.fnphoto.tv.api;

public class UrlUtils {

    public static String normalizeUrl(String input) {
        if (input.startsWith("http://") || input.startsWith("https://")) {
            String scheme = input.startsWith("https://") ? "https://" : "http://";
            String rest = input.substring(scheme.length());
            if (rest.contains("/")) rest = rest.substring(0, rest.indexOf("/"));
            if (hasPort(rest)) {
                return scheme + rest;
            } else {
                return scheme + rest + ":5666";
            }
        } else {
            if (hasPort(input)) {
                return "http://" + input;
            } else {
                return "http://" + input + ":5666";
            }
        }
    }

    public static boolean hasPort(String host) {
        if (host.startsWith("[")) {
            int closing = host.indexOf("]");
            return closing >= 0 && closing + 1 < host.length() && host.charAt(closing + 1) == ':';
        }
        return host.contains(":");
    }
}
