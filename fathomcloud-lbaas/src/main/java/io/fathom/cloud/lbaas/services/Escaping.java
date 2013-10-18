package io.fathom.cloud.lbaas.services;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class Escaping {
    public static String escape(String s) {
        String escaped;
        try {
            escaped = URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException();
        }
        escaped = escaped.replace('%', ':');
        return escaped;
    }
}
