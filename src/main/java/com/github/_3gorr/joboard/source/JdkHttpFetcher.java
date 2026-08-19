package com.github._3gorr.joboard.source;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class JdkHttpFetcher implements HttpFetcher {

    private final HttpClient client;
    private final String userAgent;
    private final Duration timeout;

    public JdkHttpFetcher(String userAgent, Duration timeout) {
        this.userAgent = userAgent;
        this.timeout = timeout;
        this.client = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public String get(String url) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout)
                .header("User-Agent", userAgent)
                .header("Accept", "application/json, text/html;q=0.9")
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return response.body();
            }
            throw new IOException("HTTP " + status + " for " + url);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while fetching " + url, e);
        }
    }
}
