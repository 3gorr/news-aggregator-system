package com.github._3gorr.joboard.service;

import com.github._3gorr.joboard.model.Salary;
import com.github._3gorr.joboard.source.RawVacancy;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ContentHasherTest {

    @Test
    void sameContentProducesSameHash() {
        RawVacancy a = sample("Java dev", "X");
        RawVacancy b = sample("Java dev", "X");
        assertThat(ContentHasher.hash(a)).isEqualTo(ContentHasher.hash(b));
    }

    @Test
    void differentTitleProducesDifferentHash() {
        RawVacancy a = sample("Java dev", "X");
        RawVacancy b = sample("Kotlin dev", "X");
        assertThat(ContentHasher.hash(a)).isNotEqualTo(ContentHasher.hash(b));
    }

    @Test
    void differentSalaryProducesDifferentHash() {
        RawVacancy a = sampleWithSalary(new Salary(100_000, 200_000, "RUB"));
        RawVacancy b = sampleWithSalary(new Salary(150_000, 200_000, "RUB"));
        assertThat(ContentHasher.hash(a)).isNotEqualTo(ContentHasher.hash(b));
    }

    @Test
    void hashIsHexSha256() {
        String hash = ContentHasher.hash(sample("X", "Y"));
        assertThat(hash).hasSize(64).matches("[0-9a-f]+");
    }

    private static RawVacancy sample(String title, String company) {
        return new RawVacancy("ext", "https://e/1", title, company, "Москва",
                Salary.none(), null, null, null,
                Instant.parse("2026-06-01T00:00:00Z"));
    }

    private static RawVacancy sampleWithSalary(Salary salary) {
        return new RawVacancy("ext", "https://e/1", "T", "C", "Москва",
                salary, null, null, null,
                Instant.parse("2026-06-01T00:00:00Z"));
    }
}
