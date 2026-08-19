package com.github._3gorr.joboard.source;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class StubHttpFetcher implements HttpFetcher {

    private final Map<String, String> responses = new HashMap<>();
    private final Map<String, IOException> failures = new HashMap<>();

    public StubHttpFetcher respondWithResource(String url, String resourcePath) {
        try (InputStream in = StubHttpFetcher.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalArgumentException("Test resource not found: " + resourcePath);
            }
            responses.put(url, new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + resourcePath, e);
        }
        return this;
    }

    public StubHttpFetcher respondWith(String url, String body) {
        responses.put(url, body);
        return this;
    }

    public StubHttpFetcher failWith(String url, IOException e) {
        failures.put(url, e);
        return this;
    }

    @Override
    public String get(String url) throws IOException {
        if (failures.containsKey(url)) {
            throw failures.get(url);
        }
        String body = responses.get(url);
        if (body == null) {
            throw new IOException("No stub for URL: " + url);
        }
        return body;
    }
}
