package com.github._3gorr.joboard.model;

import java.time.Instant;

public record Vacancy(
        Long id,
        long sourceId,
        String externalId,
        String url,
        String title,
        String company,
        String city,
        Salary salary,
        String employmentType,
        String description,
        String requirements,
        Instant publishedAt,
        Instant fetchedAt,
        String contentHash
) {
}
