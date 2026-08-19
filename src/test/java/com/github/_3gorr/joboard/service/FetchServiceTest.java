package com.github._3gorr.joboard.service;

import com.github._3gorr.joboard.TestDatabase;
import com.github._3gorr.joboard.model.Salary;
import com.github._3gorr.joboard.model.SearchFilter;
import com.github._3gorr.joboard.model.Vacancy;
import com.github._3gorr.joboard.model.VacancyHistoryEntry;
import com.github._3gorr.joboard.repository.JdbcSourceRepository;
import com.github._3gorr.joboard.repository.JdbcVacancyHistoryRepository;
import com.github._3gorr.joboard.repository.JdbcVacancyRepository;
import com.github._3gorr.joboard.repository.SourceRepository;
import com.github._3gorr.joboard.repository.VacancyHistoryRepository;
import com.github._3gorr.joboard.repository.VacancyRepository;
import com.github._3gorr.joboard.source.FetchQuery;
import com.github._3gorr.joboard.source.RawVacancy;
import com.github._3gorr.joboard.source.SourceFetchException;
import com.github._3gorr.joboard.source.SourceRegistry;
import com.github._3gorr.joboard.source.VacancySource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FetchServiceTest {

    private TestDatabase db;
    private VacancyRepository vacancyRepo;
    private VacancyHistoryRepository historyRepo;
    private SourceRepository sourceRepo;
    private FakeSource fakeHh;
    private FetchService service;

    @BeforeEach
    void setUp() {
        db = TestDatabase.create();
        vacancyRepo = new JdbcVacancyRepository(db.dataSource());
        Clock clock = Clock.fixed(Instant.parse("2026-06-05T12:00:00Z"), ZoneOffset.UTC);
        historyRepo = new JdbcVacancyHistoryRepository(db.dataSource(), clock);
        sourceRepo = new JdbcSourceRepository(db.dataSource());
        fakeHh = new FakeSource("hh");
        SourceRegistry registry = new SourceRegistry(List.of(fakeHh));
        service = new FetchService(registry, sourceRepo, vacancyRepo, historyRepo, clock);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void firstFetchInsertsAllVacancies() {
        fakeHh.feed(raw("ext-1", "Java", "Yandex", new Salary(100_000, null, "RUB")));
        fakeHh.feed(raw("ext-2", "Go", "Avito", Salary.none()));

        FetchReport report = service.fetchOne("hh", FetchQuery.defaults());

        assertThat(report.totalInserted()).isEqualTo(2);
        assertThat(report.totalUpdated()).isZero();
        assertThat(vacancyRepo.count()).isEqualTo(2L);
        assertThat(historyRepo.recent(10))
                .extracting(VacancyHistoryEntry::operation)
                .containsOnly(VacancyHistoryEntry.Operation.INSERT);
    }

    @Test
    void secondFetchWithSameContentReportsUnchanged() {
        fakeHh.feed(raw("ext-1", "Java", "Yandex", new Salary(100_000, null, "RUB")));
        service.fetchOne("hh", FetchQuery.defaults());

        fakeHh.reset();
        fakeHh.feed(raw("ext-1", "Java", "Yandex", new Salary(100_000, null, "RUB")));
        FetchReport report = service.fetchOne("hh", FetchQuery.defaults());

        assertThat(report.totalInserted()).isZero();
        assertThat(report.totalUpdated()).isZero();
        assertThat(report.totalUnchanged()).isEqualTo(1);
        assertThat(vacancyRepo.count()).isEqualTo(1L);
    }

    @Test
    void changedSalaryTriggersUpdateAndHistoryEntry() {
        fakeHh.feed(raw("ext-1", "Java", "Yandex", new Salary(100_000, null, "RUB")));
        service.fetchOne("hh", FetchQuery.defaults());

        fakeHh.reset();
        fakeHh.feed(raw("ext-1", "Java", "Yandex", new Salary(150_000, 250_000, "RUB")));
        FetchReport report = service.fetchOne("hh", FetchQuery.defaults());

        assertThat(report.totalUpdated()).isEqualTo(1);
        Vacancy updated = vacancyRepo.search(SearchFilter.builder().build()).get(0);
        assertThat(updated.salary().from()).isEqualTo(150_000);
        assertThat(updated.salary().to()).isEqualTo(250_000);

        List<VacancyHistoryEntry> events = historyRepo.recent(10);
        assertThat(events).extracting(VacancyHistoryEntry::operation)
                .containsExactly(VacancyHistoryEntry.Operation.UPDATE, VacancyHistoryEntry.Operation.INSERT);
    }

    @Test
    void disabledSourceProducesEmptyReport() {
        sourceRepo.setEnabled("hh", false);
        fakeHh.feed(raw("ext-1", "Java", "Yandex", Salary.none()));

        FetchReport report = service.fetchOne("hh", FetchQuery.defaults());

        assertThat(report.bySource()).isEmpty();
        assertThat(vacancyRepo.count()).isZero();
    }

    @Test
    void sourceFailureIsRecordedNotThrown() {
        fakeHh.failNext(new SourceFetchException("network down"));

        FetchReport report = service.fetchOne("hh", FetchQuery.defaults());

        assertThat(report.forSource("hh").failed()).isEqualTo(1);
        assertThat(vacancyRepo.count()).isZero();
    }

    @Test
    void fetchAllRunsOnlyEnabledRegisteredSources() {
        sourceRepo.setEnabled("habr_career", false);
        fakeHh.feed(raw("ext-1", "Java", "Yandex", Salary.none()));

        FetchReport report = service.fetchAll(FetchQuery.defaults());

        assertThat(report.bySource()).containsOnlyKeys("hh");
        assertThat(report.totalInserted()).isEqualTo(1);
    }

    private static RawVacancy raw(String ext, String title, String company, Salary salary) {
        return new RawVacancy(ext, "https://e/" + ext, title, company, "Москва",
                salary, null, "desc", "req",
                Instant.parse("2026-06-01T10:00:00Z"));
    }

    private static final class FakeSource implements VacancySource {
        private final String code;
        private final List<RawVacancy> queue = new ArrayList<>();
        private SourceFetchException failure;

        FakeSource(String code) {
            this.code = code;
        }

        void feed(RawVacancy v) {
            queue.add(v);
        }

        void reset() {
            queue.clear();
            failure = null;
        }

        void failNext(SourceFetchException e) {
            this.failure = e;
        }

        @Override
        public String code() {
            return code;
        }

        @Override
        public List<RawVacancy> fetch(FetchQuery query) {
            if (failure != null) {
                throw failure;
            }
            return List.copyOf(queue);
        }
    }
}
