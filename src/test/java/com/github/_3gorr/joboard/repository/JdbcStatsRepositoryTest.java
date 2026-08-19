package com.github._3gorr.joboard.repository;

import com.github._3gorr.joboard.TestDatabase;
import com.github._3gorr.joboard.model.Salary;
import com.github._3gorr.joboard.model.Vacancy;
import com.github._3gorr.joboard.service.StatsReport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class JdbcStatsRepositoryTest {

    private TestDatabase db;
    private JdbcVacancyRepository vacancyRepo;
    private StatsRepository repo;
    private long hhId;
    private long habrId;

    @BeforeEach
    void setUp() {
        db = TestDatabase.create();
        vacancyRepo = new JdbcVacancyRepository(db.dataSource());
        repo = new JdbcStatsRepository(db.dataSource());
        SourceRepository sources = new JdbcSourceRepository(db.dataSource());
        hhId = sources.findByCode("hh").orElseThrow().id();
        habrId = sources.findByCode("habr_career").orElseThrow().id();
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void countByCityRanksByFrequency() {
        seed(hhId, "a", "Yandex", "Москва", null);
        seed(hhId, "b", "Avito", "Москва", null);
        seed(hhId, "c", "Sber", "Питер", null);

        List<StatsReport.CategoryCount> rows = repo.countByCity(10);

        assertThat(rows).extracting("label", "count")
                .containsExactly(tuple("Москва", 2L), tuple("Питер", 1L));
    }

    @Test
    void countByCompanyTreatsNullAsUnknown() {
        seed(hhId, "a", "Yandex", null, null);
        seed(hhId, "b", null, null, null);

        List<StatsReport.CategoryCount> rows = repo.countByCompany(10);

        assertThat(rows).extracting("label").contains("Yandex", "(unknown)");
    }

    @Test
    void countBySourceIncludesSourcesWithZeroVacancies() {
        seed(hhId, "a", "X", null, null);

        List<StatsReport.CategoryCount> rows = repo.countBySource();

        assertThat(rows).extracting("label", "count")
                .containsExactlyInAnyOrder(tuple("hh", 1L), tuple("habr_career", 0L));
    }

    @Test
    void salaryStatsAggregatesMinMaxAvg() {
        seed(hhId, "a", "X", null, new Salary(100_000, 200_000, "RUB"));
        seed(hhId, "b", "Y", null, new Salary(200_000, 300_000, "RUB"));
        seed(hhId, "c", "Z", null, Salary.none());  // ignored

        StatsReport.SalaryStats stats = repo.salaryStats(null);

        assertThat(stats.countWithSalary()).isEqualTo(2);
        assertThat(stats.min()).isEqualTo(100_000);
        assertThat(stats.max()).isEqualTo(300_000);
        assertThat(stats.avg()).isEqualTo(200_000L);
    }

    @Test
    void salaryStatsFiltersByTitleQuery() {
        seed(hhId, "a", "X", null, new Salary(100_000, 200_000, "RUB"), "Java backend");
        seed(hhId, "b", "Y", null, new Salary(50_000, 90_000, "RUB"), "PHP junior");

        StatsReport.SalaryStats stats = repo.salaryStats("java");

        assertThat(stats.countWithSalary()).isEqualTo(1);
        assertThat(stats.min()).isEqualTo(100_000);
    }

    @Test
    void salaryStatsReturnsEmptyWhenNoSalariedVacancies() {
        seed(hhId, "a", "X", null, Salary.none());
        StatsReport.SalaryStats stats = repo.salaryStats(null);
        assertThat(stats.countWithSalary()).isZero();
    }

    private void seed(long sourceId, String ext, String company, String city, Salary salary) {
        seed(sourceId, ext, company, city, salary, "Title-" + ext);
    }

    private void seed(long sourceId, String ext, String company, String city, Salary salary, String title) {
        vacancyRepo.insert(new Vacancy(
                null, sourceId, ext, "https://e/" + ext,
                title, company, city, salary == null ? Salary.none() : salary,
                null, null, null,
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-06-05T00:00:00Z"),
                "h-" + ext));
    }
}
