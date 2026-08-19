package com.github._3gorr.joboard.export;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ExporterFactory {

    private final Map<String, Exporter> byFormat = new LinkedHashMap<>();

    public ExporterFactory(List<Exporter> exporters) {
        for (Exporter e : exporters) {
            byFormat.put(e.format().toLowerCase(), e);
        }
    }

    public static ExporterFactory defaults() {
        return new ExporterFactory(List.of(
                new CsvExporter(),
                new JsonExporter(),
                new HtmlExporter()));
    }

    public Exporter get(String format) {
        Exporter e = byFormat.get(format.toLowerCase());
        if (e == null) {
            throw new IllegalArgumentException("Unsupported export format: " + format
                    + " (supported: " + byFormat.keySet() + ")");
        }
        return e;
    }

    public Set<String> supportedFormats() {
        return byFormat.keySet();
    }
}
