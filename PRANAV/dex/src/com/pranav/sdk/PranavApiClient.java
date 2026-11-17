package com.pranav.sdk;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

public final class PranavApiClient {
    private final String baseUrl;

    public PranavApiClient(String baseUrl) {
        String trimmed = baseUrl == null ? "" : baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        this.baseUrl = trimmed;
    }

    public JSONObject postJson(String path, JSONObject payload) throws Exception {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(10000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");

        byte[] bodyBytes = payload.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(bodyBytes);
        }

        int code = conn.getResponseCode();
        InputStream stream = code >= 200 && code < 400 ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        if (stream != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }
        }
        conn.disconnect();
        JSONObject response = sb.length() > 0 ? new JSONObject(sb.toString()) : new JSONObject();
        response.put("_httpStatus", code);
        return response;
    }
}
