package com.github._3gorr.joboard.model;

import java.time.Instant;

public record NotificationFilter(
        long id,
        String name,
        String query,
        String city,
        Integer minSalary,
        Instant createdAt
) {
    public boolean matches(Vacancy v) {
        if (city != null && !city.equals(v.city())) return false;
        if (minSalary != null) {
            Integer from = v.salary() == null ? null : v.salary().from();
            Integer to = v.salary() == null ? null : v.salary().to();
            int best = Math.max(from == null ? 0 : from, to == null ? 0 : to);
            if (best < minSalary) return false;
        }
        if (query != null && !query.isBlank()) {
            String q = query.toLowerCase();
            boolean inTitle = v.title() != null && v.title().toLowerCase().contains(q);
            boolean inDesc = v.description() != null && v.description().toLowerCase().contains(q);
            boolean inReq = v.requirements() != null && v.requirements().toLowerCase().contains(q);
            if (!(inTitle || inDesc || inReq)) return false;
        }
        return true;
    }
}
