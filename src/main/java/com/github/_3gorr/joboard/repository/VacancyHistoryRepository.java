package com.github._3gorr.joboard.repository;

import com.github._3gorr.joboard.model.VacancyHistoryEntry;

import java.util.List;

public interface VacancyHistoryRepository {

    void log(Long vacancyId, long sourceId, String externalId, VacancyHistoryEntry.Operation operation);

    List<VacancyHistoryEntry> recent(int limit);
}
