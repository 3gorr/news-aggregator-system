package com.github._3gorr.joboard.repository;

import com.github._3gorr.joboard.TestDatabase;
import com.github._3gorr.joboard.model.Salary;
import com.github._3gorr.joboard.model.SearchFilter;
import com.github._3gorr.joboard.model.Vacancy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcVacancyRepositoryTest {

    private TestDatabase db;
    private VacancyRepository repo;
    private long hhId;
    private long habrId;

    @BeforeEach
    void setUp() {
        db = TestDatabase.create();
        repo = new JdbcVacancyRepository(db.dataSource());
        SourceRepository sources = new JdbcSourceRepository(db.dataSource());
        hhId = sources.findByCode("hh").orElseThrow().id();
        habrId = sources.findByCode("habr_career").orElseThrow().id();
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void insertReturnsIdAndPersistsAllFields() {
        Vacancy v = sample(hhId, "ext-1", "Backend Java", "Yandex", "Москва",
                new Salary(200_000, 350_000, "RUB"), Instant.parse("2026-06-01T10:00:00Z"));

        long id = repo.insert(v);

        Optional<Vacancy> loaded = repo.findById(id);
        assertThat(loaded).isPresent();
        Vacancy got = loaded.get();
        assertThat(got.title()).isEqualTo("Backend Java");
        assertThat(got.company()).isEqualTo("Yandex");
        assertThat(got.salary().from()).isEqualTo(200_000);
        assertThat(got.salary().to()).isEqualTo(350_000);
        assertThat(got.salary().currency()).isEqualTo("RUB");
        assertThat(got.publishedAt()).isEqualTo(Instant.parse("2026-06-01T10:00:00Z"));
    }

    @Test
    void uniqueConstraintPreventsDuplicateExternalId() {
        Vacancy v = sample(hhId, "ext-1", "Backend Java", "Yandex", "Москва",
                new Salary(200_000, null, "RUB"), Instant.parse("2026-06-01T10:00:00Z"));
        repo.insert(v);

        org.junit.jupiter.api.Assertions.assertThrows(RepositoryException.class,
                () -> repo.insert(v));
    }

    @Test
    void findByExternalIdLooksUpBySourceAndExternalId() {
        repo.insert(sample(hhId, "ext-1", "A", null, null, Salary.none(), Instant.parse("2026-01-01T00:00:00Z")));
        repo.insert(sample(habrId, "ext-1", "B", null, null, Salary.none(), Instant.parse("2026-01-01T00:00:00Z")));

        Optional<Vacancy> hhOne = repo.findByExternalId(hhId, "ext-1");
        Optional<Vacancy> habrOne = repo.findByExternalId(habrId, "ext-1");

        assertThat(hhOne).get().extracting(Vacancy::title).isEqualTo("A");
        assertThat(habrOne).get().extracting(Vacancy::title).isEqualTo("B");
    }

    @Test
    void searchFiltersByCity() {
        repo.insert(sample(hhId, "a", "Java dev", "X", "Москва", Salary.none(), Instant.parse("2026-06-01T00:00:00Z")));
        repo.insert(sample(hhId, "b", "Go dev", "Y", "Питер", Salary.none(), Instant.parse("2026-06-02T00:00:00Z")));

        List<Vacancy> moscow = repo.search(SearchFilter.builder().city("Москва").build());

        assertThat(moscow).extracting(Vacancy::title).containsExactly("Java dev");
    }

    @Test
    void searchFiltersBySalaryAndSortsBySalaryDesc() {
        repo.insert(sample(hhId, "low", "Junior", null, null,
                new Salary(50_000, 80_000, "RUB"), Instant.parse("2026-06-01T00:00:00Z")));
        repo.insert(sample(hhId, "mid", "Middle", null, null,
                new Salary(150_000, 200_000, "RUB"), Instant.parse("2026-06-02T00:00:00Z")));
        repo.insert(sample(hhId, "sen", "Senior", null, null,
                new Salary(300_000, 500_000, "RUB"), Instant.parse("2026-06-03T00:00:00Z")));

        List<Vacancy> filtered = repo.search(SearchFilter.builder()
                .minSalary(100_000)
                .sortBy(SearchFilter.SortBy.SALARY_DESC)
                .build());

        assertThat(filtered).extracting(Vacancy::title).containsExactly("Senior", "Middle");
    }

    @Test
    void searchByQueryMatchesTitleAndDescription() {
        repo.insert(sample(hhId, "a", "Java backend", null, null, Salary.none(),
                Instant.parse("2026-06-01T00:00:00Z")));
        repo.insert(withDescription(sample(hhId, "b", "Tech lead", null, null, Salary.none(),
                Instant.parse("2026-06-02T00:00:00Z")), "We use Java and Kotlin"));
        repo.insert(sample(hhId, "c", "Frontend React", null, null, Salary.none(),
                Instant.parse("2026-06-03T00:00:00Z")));

        List<Vacancy> hits = repo.search(SearchFilter.builder().query("java").build());

        assertThat(hits).extracting(Vacancy::title).containsExactlyInAnyOrder("Java backend", "Tech lead");
    }

    @Test
    void updateChangesPersistedFields() {
        long id = repo.insert(sample(hhId, "x", "Old title", "OldCo", "Москва",
                new Salary(100_000, null, "RUB"), Instant.parse("2026-06-01T00:00:00Z")));
        Vacancy original = repo.findById(id).orElseThrow();

        Vacancy updated = new Vacancy(
                original.id(), original.sourceId(), original.externalId(), original.url(),
                "New title", "NewCo", "Питер",
                new Salary(120_000, 200_000, "RUB"),
                "remote", "desc", "req",
                original.publishedAt(), Instant.parse("2026-06-05T12:00:00Z"),
                "new-hash");

        repo.update(updated);

        Vacancy reloaded = repo.findById(id).orElseThrow();
        assertThat(reloaded.title()).isEqualTo("New title");
        assertThat(reloaded.company()).isEqualTo("NewCo");
        assertThat(reloaded.city()).isEqualTo("Питер");
        assertThat(reloaded.salary().to()).isEqualTo(200_000);
        assertThat(reloaded.contentHash()).isEqualTo("new-hash");
    }

    @Test
    void deletePublishedBeforeRemovesOldRows() {
        repo.insert(sample(hhId, "old", "old", null, null, Salary.none(),
                Instant.parse("2025-01-01T00:00:00Z")));
        repo.insert(sample(hhId, "new", "new", null, null, Salary.none(),
                Instant.parse("2026-06-01T00:00:00Z")));

        int deleted = repo.deletePublishedBefore(Instant.parse("2026-01-01T00:00:00Z"));

        assertThat(deleted).isEqualTo(1);
        assertThat(repo.count()).isEqualTo(1L);
    }

    private static Vacancy sample(long sourceId, String externalId, String title,
                                  String company, String city, Salary salary, Instant published) {
        return new Vacancy(
                null, sourceId, externalId,
                "https://example.com/" + externalId,
                title, company, city, salary,
                null, null, null,
                published, Instant.parse("2026-06-05T00:00:00Z"),
                "hash-" + externalId);
    }

    private static Vacancy withDescription(Vacancy v, String desc) {
        return new Vacancy(v.id(), v.sourceId(), v.externalId(), v.url(), v.title(),
                v.company(), v.city(), v.salary(), v.employmentType(), desc,
                v.requirements(), v.publishedAt(), v.fetchedAt(), v.contentHash());
    }
}
