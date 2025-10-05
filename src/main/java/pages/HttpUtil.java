package pages;

import java.net.URI;
import java.io.IOException;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;

/**
 * Small HTTP utility wrapper around java.net.http.HttpClient for simple
 * GET/POST/DELETE
 * operations returning strings or bytes. Keeps code DRY and removes deprecated
 * URL(String)
 * usages.
 */
public class HttpUtil {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public static String getString(String url, Map<String, String> headers) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder().uri(URI.create(url)).GET()
                // per-request timeout to avoid waiting indefinitely; servers may still return
                // 408
                .timeout(Duration.ofSeconds(15));
        if (headers != null)
            headers.forEach(b::header);
        HttpRequest req = b.build();
        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        return resp.body();
    }

    public static byte[] getBytes(String url, Map<String, String> headers) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder().uri(URI.create(url)).GET()
                .timeout(Duration.ofSeconds(15));
        if (headers != null)
            headers.forEach(b::header);
        HttpRequest req = b.build();
        HttpResponse<byte[]> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofByteArray());
        return resp.body();
    }

    public static String postString(String url, String body, Map<String, String> headers) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder().uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(body)).timeout(Duration.ofSeconds(20));
        if (headers != null)
            headers.forEach(b::header);
        HttpRequest req = b.build();
        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        int code = resp.statusCode();
        String bodyResp = resp.body();
        if (code < 200 || code >= 300) {
            throw new IOException("HTTP " + code + ": " + bodyResp);
        }
        return bodyResp;
    }

    public static String delete(String url, String body, Map<String, String> headers) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(20));
        if (body != null)
            b.method("DELETE", HttpRequest.BodyPublishers.ofString(body));
        else
            b.DELETE();
        if (headers != null)
            headers.forEach(b::header);
        HttpRequest req = b.build();
        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        int code = resp.statusCode();
        String bodyResp = resp.body();
        if (code < 200 || code >= 300) {
            throw new IOException("HTTP " + code + ": " + bodyResp);
        }
        return bodyResp;
    }
}
