package com.github._3gorr.joboard.export;

import com.github._3gorr.joboard.model.Salary;
import com.github._3gorr.joboard.model.Vacancy;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public final class CsvExporter implements Exporter {

    private static final String[] HEADERS = {
            "id", "external_id", "source_id", "title", "company", "city",
            "salary_from", "salary_to", "salary_currency", "employment_type",
            "published_at", "fetched_at", "url"
    };

    @Override
    public String format() {
        return "csv";
    }

    @Override
    public void write(List<Vacancy> vacancies, Writer out) throws IOException {
        out.write(String.join(",", HEADERS));
        out.write("\n");
        for (Vacancy v : vacancies) {
            Salary s = v.salary() == null ? Salary.none() : v.salary();
            out.write(row(
                    v.id() == null ? "" : v.id().toString(),
                    v.externalId(),
                    Long.toString(v.sourceId()),
                    v.title(),
                    v.company(),
                    v.city(),
                    s.from() == null ? "" : s.from().toString(),
                    s.to() == null ? "" : s.to().toString(),
                    s.currency(),
                    v.employmentType(),
                    v.publishedAt().toString(),
                    v.fetchedAt().toString(),
                    v.url()
            ));
            out.write("\n");
        }
        out.flush();
    }

    private static String row(String... cols) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cols.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(escape(cols[i]));
        }
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        boolean needsQuoting = s.indexOf(',') >= 0
                || s.indexOf('"') >= 0
                || s.indexOf('\n') >= 0
                || s.indexOf('\r') >= 0;
        if (!needsQuoting) return s;
        return '"' + s.replace("\"", "\"\"") + '"';
    }
}
