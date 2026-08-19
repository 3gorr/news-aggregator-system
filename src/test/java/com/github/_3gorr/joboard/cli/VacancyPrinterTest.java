package com.github._3gorr.joboard.cli;

import com.github._3gorr.joboard.model.Salary;
import com.github._3gorr.joboard.model.Vacancy;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VacancyPrinterTest {

    @Test
    void printsEmptyMarkerWhenNoVacancies() {
        String out = capture(p -> VacancyPrinter.printList(List.of(), p));
        assertThat(out).contains("(no vacancies match)");
    }

    @Test
    void printListIncludesIdTitleCompanyUrl() {
        Vacancy v = sample(42L, "Java dev", "Yandex", "Москва",
                new Salary(100_000, 200_000, "RUB"));

        String out = capture(p -> VacancyPrinter.printList(List.of(v), p));

        assertThat(out)
                .contains("#42")
                .contains("Java dev")
                .contains("Yandex")
                .contains("Москва")
                .contains("100000–200000 RUB")
                .contains("https://example.com/42")
                .contains("(1 shown)");
    }

    @Test
    void formatSalaryHandlesAllCombinations() {
        assertThat(VacancyPrinter.formatSalary(new Salary(100, 200, "RUB"))).isEqualTo("100–200 RUB");
        assertThat(VacancyPrinter.formatSalary(new Salary(100, null, "RUB"))).isEqualTo("from 100 RUB");
        assertThat(VacancyPrinter.formatSalary(new Salary(null, 200, "RUB"))).isEqualTo("up to 200 RUB");
        assertThat(VacancyPrinter.formatSalary(Salary.none())).isEqualTo("salary not specified");
    }

    @Test
    void printDetailShowsAllFields() {
        Vacancy v = sample(42L, "Java dev", "Yandex", "Москва",
                new Salary(100_000, 200_000, "RUB"));

        String out = capture(p -> VacancyPrinter.printDetail(v, p));

        assertThat(out)
                .contains("ID:           42")
                .contains("Title:        Java dev")
                .contains("Company:      Yandex")
                .contains("City:         Москва")
                .contains("Salary:       100000–200000 RUB")
                .contains("URL:          https://example.com/42");
    }

    private static Vacancy sample(long id, String title, String company, String city, Salary salary) {
        return new Vacancy(id, 1L, "ext-" + id, "https://example.com/" + id,
                title, company, city, salary, "full",
                "Description here", "Requirements here",
                Instant.parse("2026-06-01T10:00:00Z"),
                Instant.parse("2026-06-05T12:00:00Z"),
                "hash");
    }

    private static String capture(java.util.function.Consumer<PrintStream> body) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (PrintStream p = new PrintStream(buf, true, StandardCharsets.UTF_8)) {
            body.accept(p);
        }
        return buf.toString(StandardCharsets.UTF_8);
    }
}
