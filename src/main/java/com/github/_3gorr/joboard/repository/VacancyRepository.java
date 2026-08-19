package com.github._3gorr.joboard.repository;

import com.github._3gorr.joboard.model.SearchFilter;
import com.github._3gorr.joboard.model.Vacancy;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface VacancyRepository {

    Optional<Vacancy> findById(long id);

    Optional<Vacancy> findByExternalId(long sourceId, String externalId);

    List<Vacancy> search(SearchFilter filter);

    long count();

    long insert(Vacancy vacancy);

    void update(Vacancy vacancy);

    int deletePublishedBefore(Instant cutoff);
}
