package com.github._3gorr.joboard.repository;

import com.github._3gorr.joboard.service.StatsReport;

import java.util.List;

public interface StatsRepository {

    List<StatsReport.CategoryCount> countByCity(int limit);

    List<StatsReport.CategoryCount> countByCompany(int limit);

    List<StatsReport.CategoryCount> countBySource();

    StatsReport.SalaryStats salaryStats(String titleQuery);
}
