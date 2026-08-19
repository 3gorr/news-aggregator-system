package com.github._3gorr.joboard.model;

import java.time.Instant;

public record VacancyHistoryEntry(
        long id,
        Long vacancyId,
        long sourceId,
        String externalId,
        Operation operation,
        Instant occurredAt
) {

    public enum Operation {
        INSERT, UPDATE, DELETE
    }
}
