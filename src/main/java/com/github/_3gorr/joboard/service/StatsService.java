package com.github._3gorr.joboard.service;

import com.github._3gorr.joboard.repository.StatsRepository;

import java.util.List;

public final class StatsService {

    private final StatsRepository repository;

    public StatsService(StatsRepository repository) {
        this.repository = repository;
    }

    public List<StatsReport.CategoryCount> byCity(int limit) {
        return repository.countByCity(limit);
    }

    public List<StatsReport.CategoryCount> byCompany(int limit) {
        return repository.countByCompany(limit);
    }

    public List<StatsReport.CategoryCount> bySource() {
        return repository.countBySource();
    }

    public StatsReport.SalaryStats salaryStats(String titleQuery) {
        return repository.salaryStats(titleQuery);
    }
}
