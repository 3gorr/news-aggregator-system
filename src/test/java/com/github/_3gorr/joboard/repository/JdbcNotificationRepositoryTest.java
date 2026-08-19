package com.github._3gorr.joboard.repository;

import com.github._3gorr.joboard.TestDatabase;
import com.github._3gorr.joboard.model.NotificationFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcNotificationRepositoryTest {

    private TestDatabase db;
    private NotificationRepository repo;

    @BeforeEach
    void setUp() {
        db = TestDatabase.create();
        Clock clock = Clock.fixed(Instant.parse("2026-06-05T12:00:00Z"), ZoneOffset.UTC);
        repo = new JdbcNotificationRepository(db.dataSource(), clock);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void addAndListReturnsTheSavedFilter() {
        long id = repo.add("java-msk", "java", "Москва", 200_000);

        List<NotificationFilter> all = repo.findAll();

        assertThat(all).hasSize(1);
        NotificationFilter f = all.get(0);
        assertThat(f.id()).isEqualTo(id);
        assertThat(f.name()).isEqualTo("java-msk");
        assertThat(f.query()).isEqualTo("java");
        assertThat(f.city()).isEqualTo("Москва");
        assertThat(f.minSalary()).isEqualTo(200_000);
        assertThat(f.createdAt()).isEqualTo(Instant.parse("2026-06-05T12:00:00Z"));
    }

    @Test
    void addAcceptsNullOptionalFields() {
        repo.add("any", null, null, null);
        NotificationFilter f = repo.findByName("any").orElseThrow();
        assertThat(f.query()).isNull();
        assertThat(f.city()).isNull();
        assertThat(f.minSalary()).isNull();
    }

    @Test
    void removeReturnsTrueWhenDeleted() {
        repo.add("x", null, null, null);
        assertThat(repo.removeByName("x")).isTrue();
        assertThat(repo.findAll()).isEmpty();
    }

    @Test
    void removeReturnsFalseWhenNothingToDelete() {
        assertThat(repo.removeByName("nope")).isFalse();
    }
}
