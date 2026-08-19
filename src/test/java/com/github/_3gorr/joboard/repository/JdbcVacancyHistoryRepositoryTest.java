package com.github._3gorr.joboard.repository;

import com.github._3gorr.joboard.TestDatabase;
import com.github._3gorr.joboard.model.VacancyHistoryEntry;
import com.github._3gorr.joboard.model.VacancyHistoryEntry.Operation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcVacancyHistoryRepositoryTest {

    private TestDatabase db;
    private VacancyHistoryRepository repo;

    @BeforeEach
    void setUp() {
        db = TestDatabase.create();
        Clock fixed = Clock.fixed(Instant.parse("2026-06-05T12:00:00Z"), ZoneOffset.UTC);
        repo = new JdbcVacancyHistoryRepository(db.dataSource(), fixed);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void logsOperationAndReturnsItFromRecent() {
        repo.log(42L, 1L, "ext-1", Operation.INSERT);

        List<VacancyHistoryEntry> recent = repo.recent(10);

        assertThat(recent).hasSize(1);
        VacancyHistoryEntry e = recent.get(0);
        assertThat(e.vacancyId()).isEqualTo(42L);
        assertThat(e.externalId()).isEqualTo("ext-1");
        assertThat(e.operation()).isEqualTo(Operation.INSERT);
        assertThat(e.occurredAt()).isEqualTo(Instant.parse("2026-06-05T12:00:00Z"));
    }

    @Test
    void supportsNullVacancyIdForDeleteEvents() {
        repo.log(null, 1L, "ext-gone", Operation.DELETE);

        List<VacancyHistoryEntry> recent = repo.recent(10);

        assertThat(recent).hasSize(1);
        assertThat(recent.get(0).vacancyId()).isNull();
        assertThat(recent.get(0).operation()).isEqualTo(Operation.DELETE);
    }

    @Test
    void recentRespectsLimit() {
        for (int i = 0; i < 5; i++) {
            repo.log((long) i, 1L, "ext-" + i, Operation.INSERT);
        }
        assertThat(repo.recent(3)).hasSize(3);
    }
}
