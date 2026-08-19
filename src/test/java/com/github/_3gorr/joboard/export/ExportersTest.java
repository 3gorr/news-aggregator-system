package com.github._3gorr.joboard.export;

import com.github._3gorr.joboard.model.Salary;
import com.github._3gorr.joboard.model.Vacancy;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExportersTest {

    private final List<Vacancy> sample = List.of(
            new Vacancy(1L, 1L, "ext-1", "https://e/1",
                    "Java, Senior", "Yandex, LLC", "Москва",
                    new Salary(200_000, 300_000, "RUB"), "full",
                    "Description with \"quotes\"", "Reqs",
                    Instant.parse("2026-06-01T10:00:00Z"),
                    Instant.parse("2026-06-05T12:00:00Z"),
                    "hash1"),
            new Vacancy(2L, 1L, "ext-2", "https://e/2?a=1&b=2",
                    "Go dev", null, "Питер",
                    Salary.none(), null, null, null,
                    Instant.parse("2026-06-02T10:00:00Z"),
                    Instant.parse("2026-06-05T12:00:00Z"),
                    "hash2"));

    @Test
    void csvHasHeaderAndQuotesValuesWithCommas() throws IOException {
        StringWriter sw = new StringWriter();
        new CsvExporter().write(sample, sw);

        String out = sw.toString();
        String[] lines = out.split("\n");
        assertThat(lines[0]).startsWith("id,external_id,source_id,title,company,city");
        // values containing commas must be wrapped in quotes
        assertThat(lines[1]).contains("\"Java, Senior\"")
                .contains("\"Yandex, LLC\"")
                .contains("Москва");
        // empty company column must produce two consecutive commas
        assertThat(lines[2]).contains("Go dev").contains(",,");
    }

    @Test
    void csvHandlesEmptyList() throws IOException {
        StringWriter sw = new StringWriter();
        new CsvExporter().write(List.of(), sw);
        assertThat(sw.toString()).startsWith("id,external_id");
    }

    @Test
    void jsonProducesValidArrayAndOmitsNulls() throws IOException {
        StringWriter sw = new StringWriter();
        new JsonExporter().write(sample, sw);

        String out = sw.toString();
        assertThat(out.trim()).startsWith("[").endsWith("]");
        assertThat(out).contains("\"externalId\" : \"ext-1\"")
                .contains("\"title\" : \"Java, Senior\"")
                .contains("\"publishedAt\" : \"2026-06-01T10:00:00Z\"");
        // null company in the second vacancy must be omitted, not written as null
        assertThat(out).doesNotContain("\"company\" : null");
    }

    @Test
    void htmlEscapesSpecialCharactersAndIncludesLinks() throws IOException {
        StringWriter sw = new StringWriter();
        new HtmlExporter().write(sample, sw);

        String out = sw.toString();
        assertThat(out).startsWith("<!DOCTYPE html>");
        assertThat(out).contains("<table>")
                .contains("Java, Senior")
                .contains("https://e/2?a=1&amp;b=2")
                .contains("200000–300000 RUB")
                .contains("(2 vacancies)");
    }

    @Test
    void htmlEscapesAngleBracketsAndQuotesInTitle() throws IOException {
        Vacancy evil = new Vacancy(99L, 1L, "x", "https://e/x",
                "<script>alert(\"xss\")</script>", "C&Co", "City",
                Salary.none(), null, null, null,
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-06-05T00:00:00Z"), "h");
        StringWriter sw = new StringWriter();
        new HtmlExporter().write(List.of(evil), sw);

        String out = sw.toString();
        assertThat(out).doesNotContain("<script>");
        assertThat(out).contains("&lt;script&gt;");
        assertThat(out).contains("&quot;xss&quot;");
        assertThat(out).contains("C&amp;Co");
    }

    @Test
    void factoryReturnsExporterByFormatCaseInsensitively() {
        ExporterFactory f = ExporterFactory.defaults();
        assertThat(f.get("csv")).isInstanceOf(CsvExporter.class);
        assertThat(f.get("JSON")).isInstanceOf(JsonExporter.class);
        assertThat(f.get("Html")).isInstanceOf(HtmlExporter.class);
    }

    @Test
    void factoryThrowsForUnknownFormat() {
        assertThatThrownBy(() -> ExporterFactory.defaults().get("xml"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("xml");
    }
}
