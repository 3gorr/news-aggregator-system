package com.github._3gorr.joboard.db;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppConfig {

    private final Properties properties;

    private AppConfig(Properties properties) {
        this.properties = properties;
    }

    public static AppConfig load() {
        Properties props = new Properties();
        try (InputStream in = AppConfig.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load application.properties", e);
        }
        for (String key : props.stringPropertyNames()) {
            String envOverride = System.getProperty(key);
            if (envOverride != null) {
                props.setProperty(key, envOverride);
            }
        }
        return new AppConfig(props);
    }

    public static AppConfig forTest(String dbUrl) {
        Properties p = new Properties();
        p.setProperty("db.url", dbUrl);
        p.setProperty("db.poolSize", "2");
        p.setProperty("http.timeoutSeconds", "5");
        p.setProperty("http.userAgent", "joboard-test/0.1");
        return new AppConfig(p);
    }

    public String dbUrl() {
        return required("db.url");
    }

    public int dbPoolSize() {
        return Integer.parseInt(properties.getProperty("db.poolSize", "4"));
    }

    public int httpTimeoutSeconds() {
        return Integer.parseInt(properties.getProperty("http.timeoutSeconds", "15"));
    }

    public String httpUserAgent() {
        return properties.getProperty("http.userAgent", "joboard/0.1");
    }

    private String required(String key) {
        String v = properties.getProperty(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing required property: " + key);
        }
        return v;
    }
}
