package com.github._3gorr.joboard.service;

import com.github._3gorr.joboard.model.Salary;
import com.github._3gorr.joboard.model.SearchFilter;
import com.github._3gorr.joboard.model.Vacancy;
import com.github._3gorr.joboard.repository.VacancyRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchServiceTest {

    @Test
    void delegatesSearchToRepository() {
        VacancyRepository repo = mock(VacancyRepository.class);
        SearchFilter filter = SearchFilter.builder().query("java").build();
        when(repo.search(filter)).thenReturn(List.of());

        new SearchService(repo).search(filter);

        verify(repo).search(filter);
    }

    @Test
    void findByIdReturnsEmptyWhenAbsent() {
        VacancyRepository repo = mock(VacancyRepository.class);
        when(repo.findById(any(Long.class))).thenReturn(Optional.empty());

        SearchService service = new SearchService(repo);

        assertThat(service.findById(42L)).isEmpty();
    }

    @Test
    void totalCountDelegates() {
        VacancyRepository repo = mock(VacancyRepository.class);
        when(repo.count()).thenReturn(7L);

        assertThat(new SearchService(repo).totalCount()).isEqualTo(7L);
    }

    @Test
    void findByIdReturnsRepositoryVacancy() {
        Vacancy stub = new Vacancy(10L, 1L, "ext", "https://e/10",
                "Title", "Co", "City", Salary.none(), null, null, null,
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-06-05T00:00:00Z"), "hash");
        VacancyRepository repo = mock(VacancyRepository.class);
        when(repo.findById(10L)).thenReturn(Optional.of(stub));

        assertThat(new SearchService(repo).findById(10L)).contains(stub);
    }
}
