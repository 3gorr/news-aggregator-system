package com.github._3gorr.joboard.repository;

import com.github._3gorr.joboard.TestDatabase;
import com.github._3gorr.joboard.model.Source;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcSourceRepositoryTest {

    private TestDatabase db;
    private SourceRepository repo;

    @BeforeEach
    void setUp() {
        db = TestDatabase.create();
        repo = new JdbcSourceRepository(db.dataSource());
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void seedsTwoSources() {
        List<Source> all = repo.findAll();
        assertThat(all).extracting(Source::code).containsExactlyInAnyOrder("hh", "habr_career");
        assertThat(all).allMatch(Source::enabled);
    }

    @Test
    void findByCodeReturnsSource() {
        Optional<Source> hh = repo.findByCode("hh");
        assertThat(hh).isPresent();
        assertThat(hh.get().baseUrl()).isEqualTo("https://api.hh.ru");
    }

    @Test
    void findByCodeReturnsEmptyForUnknown() {
        assertThat(repo.findByCode("missing")).isEmpty();
    }

    @Test
    void disableMovesSourceOutOfEnabledList() {
        repo.setEnabled("hh", false);
        assertThat(repo.findEnabled()).extracting(Source::code).containsExactly("habr_career");
        assertThat(repo.findByCode("hh")).get().extracting(Source::enabled).isEqualTo(false);
    }

    @Test
    void setEnabledOnUnknownThrows() {
        assertThatThrownBy(() -> repo.setEnabled("nope", true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
