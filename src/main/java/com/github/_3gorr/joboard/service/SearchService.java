package com.github._3gorr.joboard.service;

import com.github._3gorr.joboard.model.SearchFilter;
import com.github._3gorr.joboard.model.Vacancy;
import com.github._3gorr.joboard.repository.VacancyRepository;

import java.util.List;
import java.util.Optional;

public final class SearchService {

    private final VacancyRepository repository;

    public SearchService(VacancyRepository repository) {
        this.repository = repository;
    }

    public List<Vacancy> search(SearchFilter filter) {
        return repository.search(filter);
    }

    public Optional<Vacancy> findById(long id) {
        return repository.findById(id);
    }

    public long totalCount() {
        return repository.count();
    }
}
