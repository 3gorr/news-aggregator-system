package com.github._3gorr.joboard.service;

import com.github._3gorr.joboard.model.Salary;
import com.github._3gorr.joboard.model.Vacancy;
import com.github._3gorr.joboard.repository.NotificationRepository;
import com.github._3gorr.joboard.repository.RepositoryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private NotificationRepository repo;
    private ByteArrayOutputStream buf;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        repo = mock(NotificationRepository.class);
        buf = new ByteArrayOutputStream();
        service = new NotificationService(repo, new PrintStream(buf, true, StandardCharsets.UTF_8));
    }

    @Test
    void onInsertedPrintsLineForEveryMatchingFilter() {
        when(repo.findAll()).thenReturn(List.of(
                new com.github._3gorr.joboard.model.NotificationFilter(
                        1L, "java-msk", "java", "Москва", null, Instant.parse("2026-06-01T00:00:00Z")),
                new com.github._3gorr.joboard.model.NotificationFilter(
                        2L, "high", null, null, 500_000, Instant.parse("2026-06-01T00:00:00Z"))));

        service.onInserted(vacancy("Java senior", "Yandex", "Москва",
                new Salary(200_000, 300_000, "RUB")));

        String out = buf.toString(StandardCharsets.UTF_8);
        assertThat(out).contains("[notify:java-msk]")
                .contains("Java senior")
                .doesNotContain("[notify:high]");
    }

    @Test
    void onUpdatedPrintsUpdatedLabel() {
        when(repo.findAll()).thenReturn(List.of(
                new com.github._3gorr.joboard.model.NotificationFilter(
                        1L, "any", null, null, null, Instant.parse("2026-06-01T00:00:00Z"))));

        service.onUpdated(vacancy("X", "Y", null, Salary.none()));

        assertThat(buf.toString(StandardCharsets.UTF_8)).contains("UPDATED");
    }

    @Test
    void addDelegatesToRepository() {
        when(repo.add("n", "q", "c", 100)).thenReturn(42L);
        assertThat(service.add("n", "q", "c", 100)).isEqualTo(42L);
    }

    @Test
    void repositoryFailurePropagates() {
        when(repo.add(any(), any(), any(), any()))
                .thenThrow(new RepositoryException("boom", null));
        assertThatThrownBy(() -> service.add("n", null, null, null))
                .isInstanceOf(RepositoryException.class);
    }

    private static Vacancy vacancy(String title, String company, String city, Salary salary) {
        return new Vacancy(1L, 1L, "ext", "https://e/1",
                title, company, city, salary, null, null, null,
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-06-05T00:00:00Z"), "h");
    }
}
