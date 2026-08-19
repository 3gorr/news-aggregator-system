package com.github._3gorr.joboard.source;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SourceRegistry {

    private final Map<String, VacancySource> byCode = new LinkedHashMap<>();

    public SourceRegistry(List<VacancySource> sources) {
        for (VacancySource s : sources) {
            byCode.put(s.code(), s);
        }
    }

    public Collection<VacancySource> all() {
        return byCode.values();
    }

    public Optional<VacancySource> find(String code) {
        return Optional.ofNullable(byCode.get(code));
    }
}
