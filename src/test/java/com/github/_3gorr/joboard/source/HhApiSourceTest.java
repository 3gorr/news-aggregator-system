package com.github._3gorr.joboard.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HhApiSourceTest {

    private static final String BASE = "https://test.hh";

    @Test
    void parsesItemsFromFixture() {
        StubHttpFetcher http = new StubHttpFetcher()
                .respondWithResource(BASE + "/vacancies?page=0&per_page=20", "/fixtures/hh-page-0.json");
        HhApiSource source = new HhApiSource(http, new ObjectMapper(), BASE);

        List<RawVacancy> got = source.fetch(FetchQuery.defaults());

        assertThat(got).hasSize(3);
        RawVacancy first = got.get(0);
        assertThat(first.externalId()).isEqualTo("100100001");
        assertThat(first.title()).isEqualTo("Java Backend Developer");
        assertThat(first.company()).isEqualTo("Yandex");
        assertThat(first.city()).isEqualTo("Москва");
        assertThat(first.salary().from()).isEqualTo(250_000);
        assertThat(first.salary().to()).isEqualTo(400_000);
        assertThat(first.salary().currency()).isEqualTo("RUB");
        assertThat(first.url()).isEqualTo("https://hh.ru/vacancy/100100001");
        assertThat(first.publishedAt()).isEqualTo(Instant.parse("2026-06-01T07:00:00Z"));
    }

    @Test
    void handlesMissingSalary() {
        StubHttpFetcher http = new StubHttpFetcher()
                .respondWithResource(BASE + "/vacancies?page=0&per_page=20", "/fixtures/hh-page-0.json");
        HhApiSource source = new HhApiSource(http, new ObjectMapper(), BASE);

        RawVacancy goDev = source.fetch(FetchQuery.defaults()).get(1);

        assertThat(goDev.title()).isEqualTo("Go Developer");
        assertThat(goDev.salary().isEmpty()).isTrue();
    }

    @Test
    void handlesPartialSalary() {
        StubHttpFetcher http = new StubHttpFetcher()
                .respondWithResource(BASE + "/vacancies?page=0&per_page=20", "/fixtures/hh-page-0.json");
        HhApiSource source = new HhApiSource(http, new ObjectMapper(), BASE);

        RawVacancy phpJun = source.fetch(FetchQuery.defaults()).get(2);

        assertThat(phpJun.salary().from()).isEqualTo(60_000);
        assertThat(phpJun.salary().to()).isNull();
    }

    @Test
    void encodesTextQuery() {
        StubHttpFetcher http = new StubHttpFetcher()
                .respondWith(BASE + "/vacancies?page=0&per_page=20&text=java+developer",
                        "{\"items\":[],\"pages\":1}");
        HhApiSource source = new HhApiSource(http, new ObjectMapper(), BASE);

        List<RawVacancy> got = source.fetch(FetchQuery.of("java developer", 1, 20));

        assertThat(got).isEmpty();
    }

    @Test
    void wrapsHttpFailureInSourceException() {
        StubHttpFetcher http = new StubHttpFetcher()
                .failWith(BASE + "/vacancies?page=0&per_page=20",
                        new java.io.IOException("boom"));
        HhApiSource source = new HhApiSource(http, new ObjectMapper(), BASE);

        assertThatThrownBy(() -> source.fetch(FetchQuery.defaults()))
                .isInstanceOf(SourceFetchException.class)
                .hasMessageContaining("HH API request failed");
    }
}
