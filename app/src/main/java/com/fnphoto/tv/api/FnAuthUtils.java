package com.fnphoto.tv.api;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class FnAuthUtils {
    private static final String TAG = "FnAuthUtils";
    private static final String API_KEY = "NDzZTVxnRKP8Z0jXg1VAMonaG8akvh";
    private static final String API_SECRET = "EAECCF25-80A6-4666-A7C2-A76904A74AB6";

    /**
     * 生成 AuthX 请求头
     * @param url 请求路径 (/p/api/v1/xxx)
     * @param method HTTP方法 (GET/POST)
     * @param data GET请求时为查询参数字符串(如"a=1&b=2")，POST请求时为body内容
     * @return authx 字符串
     */
    public static String generateAuthX(String url, String method, String data) {
        try {
            String path = url;
            String sortedParams = "";

            // 1. 生成随机 nonce (6位随机数)
            String nonce = generateNonce();

            // 2. 获取当前时间戳 (毫秒)
            String timestamp = String.valueOf(System.currentTimeMillis());

            // 3. 计算 payload_hash
            String body = data == null ? "" : data;
            String payloadHash = "";

            if ("GET".equalsIgnoreCase(method)) {
                if (data == null) {
                    payloadHash = md5("");
                } else {
                    sortedParams = buildSortedQueryString(data);
                    payloadHash = md5(sortedParams);

                    Log.d(TAG, "SortedParams:" + sortedParams);
                }
            } else {
                payloadHash = md5(body);
            }

            // 4. 拼接签名串
            String signStr = API_KEY + "_" + path + "_" + nonce + "_" + timestamp + "_" + payloadHash + "_" + API_SECRET;

//            Log.d(TAG, "Sign string: " + signStr);

            // 5. 计算签名
            String sign = md5(signStr);

            // 6. 组合 authx
            return "nonce=" + nonce + "&timestamp=" + timestamp + "&sign=" + sign;

        } catch (Exception e) {
            Log.e(TAG, "generateAuthX error", e);
            return null;
        }
    }

    /**
     * 将查询参数排序并拼接成字符串
     * @param query 查询参数字符串 (如 "b=2&a=1")
     * @return 排序后的参数字符串 (如 "a=1&b=2")
     */
    private static String buildSortedQueryString(String query) throws JSONException {
        if (query == null || query.isEmpty()) {
            return "";
        }

        // 解析参数
        JSONObject json = new JSONObject();
        String[] pairs = query.split("&");

        for (String pair : pairs) {
            int eqIndex = pair.indexOf("=");
            if (eqIndex > 0) {
                String key = pair.substring(0, eqIndex);
                String value = eqIndex < pair.length() - 1 ? pair.substring(eqIndex + 1) : "";

                // 排除 null 和 undefined
                if (!"null".equals(value) && !"undefined".equals(value)) {
                    json.put(key, value);
                }
            }
        }

        // 按键排序
        List<String> keys = new ArrayList<>();
        Iterator<String> iterator = json.keys();
        while (iterator.hasNext()) {
            keys.add(iterator.next());
        }
        Collections.sort(keys);

        // 拼接成字符串
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) sb.append("&");
            String key = keys.get(i);
            sb.append(key).append("=").append(json.getString(key));
        }

        return sb.toString();
    }

    /**
     * 生成6位随机数作为 nonce
     */
    private static String generateNonce() {
        SecureRandom random = new SecureRandom();
        int nonce = 100000 + random.nextInt(900000); // 6位数字
        return String.valueOf(nonce);
    }

    /**
     * MD5 哈希
     * @param input 输入字符串
     * @return 32位小写十六进制字符串
     */
    private static String md5(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hash = md.digest(input.getBytes("UTF-8"));
        return bytesToHex(hash);
    }

    /**
     * 字节数组转十六进制
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
