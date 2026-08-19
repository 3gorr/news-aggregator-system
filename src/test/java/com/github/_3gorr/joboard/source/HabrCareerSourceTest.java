package com.github._3gorr.joboard.source;

import com.github._3gorr.joboard.model.Salary;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HabrCareerSourceTest {

    private static final String BASE = "https://test.habr";

    @Test
    void parsesCardsFromFixture() {
        StubHttpFetcher http = new StubHttpFetcher()
                .respondWithResource(BASE + "/vacancies?page=1", "/fixtures/habr-page-1.html");
        HabrCareerSource source = new HabrCareerSource(http, BASE);

        List<RawVacancy> got = source.fetch(FetchQuery.defaults());

        assertThat(got).hasSize(3);
        RawVacancy kotlin = got.get(0);
        assertThat(kotlin.externalId()).isEqualTo("1000123456");
        assertThat(kotlin.title()).isEqualTo("Senior Kotlin Developer");
        assertThat(kotlin.company()).isEqualTo("JetBrains");
        assertThat(kotlin.city()).isEqualTo("Москва");
        assertThat(kotlin.salary().from()).isEqualTo(300_000);
        assertThat(kotlin.salary().currency()).isEqualTo("RUB");
        assertThat(kotlin.url()).isEqualTo(BASE + "/vacancies/1000123456");
        assertThat(kotlin.publishedAt()).isEqualTo(Instant.parse("2026-06-04T00:00:00Z"));
    }

    @Test
    void parsesSalaryRange() {
        StubHttpFetcher http = new StubHttpFetcher()
                .respondWithResource(BASE + "/vacancies?page=1", "/fixtures/habr-page-1.html");
        HabrCareerSource source = new HabrCareerSource(http, BASE);

        RawVacancy react = source.fetch(FetchQuery.defaults()).get(1);

        assertThat(react.salary().from()).isEqualTo(200_000);
        assertThat(react.salary().to()).isEqualTo(350_000);
        assertThat(react.salary().currency()).isEqualTo("RUB");
    }

    @Test
    void parsesUsdSalary() {
        StubHttpFetcher http = new StubHttpFetcher()
                .respondWithResource(BASE + "/vacancies?page=1", "/fixtures/habr-page-1.html");
        HabrCareerSource source = new HabrCareerSource(http, BASE);

        RawVacancy data = source.fetch(FetchQuery.defaults()).get(2);

        assertThat(data.salary().from()).isEqualTo(4000);
        assertThat(data.salary().to()).isNull();
        assertThat(data.salary().currency()).isEqualTo("USD");
    }

    @Test
    void chipLayoutExtractsCityFromPlacemarkChip() {
        StubHttpFetcher http = new StubHttpFetcher()
                .respondWithResource(BASE + "/vacancies?page=1", "/fixtures/habr-chips-page.html");
        HabrCareerSource source = new HabrCareerSource(http, BASE);

        List<RawVacancy> got = source.fetch(FetchQuery.defaults());

        assertThat(got).hasSize(2);
        assertThat(got.get(0).city()).isEqualTo("Томск");
        assertThat(got.get(0).salary().from()).isEqualTo(250_000);
    }

    @Test
    void chipLayoutWithoutPlacemarkChipMeansNoCity() {
        StubHttpFetcher http = new StubHttpFetcher()
                .respondWithResource(BASE + "/vacancies?page=1", "/fixtures/habr-chips-page.html");
        HabrCareerSource source = new HabrCareerSource(http, BASE);

        RawVacancy remote = source.fetch(FetchQuery.defaults()).get(1);

        assertThat(remote.city()).isNull();
    }

    @Test
    void predictedSalaryBlockIsIgnored() {
        StubHttpFetcher http = new StubHttpFetcher()
                .respondWithResource(BASE + "/vacancies?page=1", "/fixtures/habr-chips-page.html");
        HabrCareerSource source = new HabrCareerSource(http, BASE);

        RawVacancy noSalary = source.fetch(FetchQuery.defaults()).get(1);

        assertThat(noSalary.salary().isEmpty()).isTrue();
    }

    @Test
    void parseSalary_handlesVariousFormats() {
        Salary fromOnly = HabrCareerSource.parseSalary("от 150 000 ₽");
        assertThat(fromOnly.from()).isEqualTo(150_000);
        assertThat(fromOnly.to()).isNull();

        Salary toOnly = HabrCareerSource.parseSalary("до 100 000 руб");
        assertThat(toOnly.from()).isNull();
        assertThat(toOnly.to()).isEqualTo(100_000);

        Salary range = HabrCareerSource.parseSalary("100 000 – 200 000 ₽");
        assertThat(range.from()).isEqualTo(100_000);
        assertThat(range.to()).isEqualTo(200_000);

        Salary euro = HabrCareerSource.parseSalary("от 5000 €");
        assertThat(euro.currency()).isEqualTo("EUR");

        Salary empty = HabrCareerSource.parseSalary(null);
        assertThat(empty.isEmpty()).isTrue();
    }
}
