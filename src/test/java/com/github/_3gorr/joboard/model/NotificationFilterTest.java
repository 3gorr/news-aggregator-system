package com.github._3gorr.joboard.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationFilterTest {

    @Test
    void matchesEverythingWhenAllCriteriaNull() {
        NotificationFilter f = filter(null, null, null);
        assertThat(f.matches(vacancy("Java", "Yandex", "Москва", null))).isTrue();
    }

    @Test
    void cityFilterIsExactMatch() {
        NotificationFilter f = filter(null, "Москва", null);
        assertThat(f.matches(vacancy("Any", "Any", "Москва", null))).isTrue();
        assertThat(f.matches(vacancy("Any", "Any", "Питер", null))).isFalse();
    }

    @Test
    void minSalaryMatchesWhenEitherBoundReachesIt() {
        NotificationFilter f = filter(null, null, 200_000);
        assertThat(f.matches(vacancy("X", "Y", null, new Salary(150_000, 250_000, "RUB")))).isTrue();
        assertThat(f.matches(vacancy("X", "Y", null, new Salary(100_000, 180_000, "RUB")))).isFalse();
        assertThat(f.matches(vacancy("X", "Y", null, new Salary(null, 220_000, "RUB")))).isTrue();
    }

    @Test
    void minSalaryRejectsWhenNoSalaryAtAll() {
        NotificationFilter f = filter(null, null, 100_000);
        assertThat(f.matches(vacancy("X", "Y", null, Salary.none()))).isFalse();
    }

    @Test
    void queryMatchesTitleDescriptionOrRequirements() {
        NotificationFilter f = filter("java", null, null);
        assertThat(f.matches(vacancy("Java backend", null, null, null))).isTrue();

        Vacancy descOnly = new Vacancy(1L, 1L, "x", "u", "Backend lead", null, null,
                Salary.none(), null, "We use Java a lot", null,
                Instant.parse("2026-06-01T00:00:00Z"), Instant.parse("2026-06-05T00:00:00Z"), "h");
        assertThat(f.matches(descOnly)).isTrue();

        Vacancy noMatch = vacancy("Go developer", null, null, null);
        assertThat(f.matches(noMatch)).isFalse();
    }

    @Test
    void allCriteriaMustMatchSimultaneously() {
        NotificationFilter f = filter("java", "Москва", 150_000);
        assertThat(f.matches(vacancy("Java backend", null, "Москва",
                new Salary(200_000, null, "RUB")))).isTrue();
        assertThat(f.matches(vacancy("Java backend", null, "Питер",
                new Salary(200_000, null, "RUB")))).isFalse();
        assertThat(f.matches(vacancy("Java backend", null, "Москва",
                new Salary(100_000, null, "RUB")))).isFalse();
        assertThat(f.matches(vacancy("Go", null, "Москва",
                new Salary(200_000, null, "RUB")))).isFalse();
    }

    private static NotificationFilter filter(String query, String city, Integer minSalary) {
        return new NotificationFilter(1L, "test", query, city, minSalary,
                Instant.parse("2026-06-01T00:00:00Z"));
    }

    private static Vacancy vacancy(String title, String company, String city, Salary salary) {
        return new Vacancy(1L, 1L, "ext", "u", title, company, city,
                salary == null ? Salary.none() : salary, null, null, null,
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-06-05T00:00:00Z"), "h");
    }
}
