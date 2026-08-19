package com.github._3gorr.joboard.service;

import java.util.LinkedHashMap;
import java.util.Map;

public final class FetchReport {

    private final Map<String, SourceStats> bySource = new LinkedHashMap<>();

    public SourceStats forSource(String code) {
        return bySource.computeIfAbsent(code, k -> new SourceStats());
    }

    public Map<String, SourceStats> bySource() {
        return bySource;
    }

    public int totalInserted() {
        return bySource.values().stream().mapToInt(s -> s.inserted).sum();
    }

    public int totalUpdated() {
        return bySource.values().stream().mapToInt(s -> s.updated).sum();
    }

    public int totalUnchanged() {
        return bySource.values().stream().mapToInt(s -> s.unchanged).sum();
    }

    public int totalFailed() {
        return bySource.values().stream().mapToInt(s -> s.failed).sum();
    }

    public static final class SourceStats {
        int inserted;
        int updated;
        int unchanged;
        int failed;

        public int inserted() { return inserted; }
        public int updated() { return updated; }
        public int unchanged() { return unchanged; }
        public int failed() { return failed; }
        public int seen() { return inserted + updated + unchanged + failed; }
    }
}
