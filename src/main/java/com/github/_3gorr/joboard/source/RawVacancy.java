package com.github._3gorr.joboard.source;

import com.github._3gorr.joboard.model.Salary;

import java.time.Instant;

public record RawVacancy(
        String externalId,
        String url,
        String title,
        String company,
        String city,
        Salary salary,
        String employmentType,
        String description,
        String requirements,
        Instant publishedAt
) {
    public RawVacancy {
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalArgumentException("externalId is required");
        }
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("url is required");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        if (publishedAt == null) {
            throw new IllegalArgumentException("publishedAt is required");
        }
        if (salary == null) {
            salary = Salary.none();
        }
    }
}
