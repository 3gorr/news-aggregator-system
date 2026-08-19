package com.github._3gorr.joboard.cli;

import com.github._3gorr.joboard.model.Salary;
import com.github._3gorr.joboard.model.Vacancy;

import java.io.PrintStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class VacancyPrinter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault());

    private VacancyPrinter() {
    }

    public static void printList(List<Vacancy> vacancies, PrintStream out) {
        if (vacancies.isEmpty()) {
            out.println("(no vacancies match)");
            return;
        }
        for (Vacancy v : vacancies) {
            out.printf("#%-6d [%s]  %s%n",
                    v.id(), DATE.format(v.publishedAt()), v.title());
            out.printf("        %s%s · %s%n",
                    v.company() == null ? "(unknown)" : v.company(),
                    v.city() == null ? "" : " · " + v.city(),
                    formatSalary(v.salary()));
            out.printf("        %s%n", v.url());
        }
        out.printf("%n(%d shown)%n", vacancies.size());
    }

    public static void printDetail(Vacancy v, PrintStream out) {
        out.printf("ID:           %d%n", v.id());
        out.printf("Title:        %s%n", v.title());
        out.printf("Company:      %s%n", nvl(v.company()));
        out.printf("City:         %s%n", nvl(v.city()));
        out.printf("Salary:       %s%n", formatSalary(v.salary()));
        out.printf("Employment:   %s%n", nvl(v.employmentType()));
        out.printf("Published:    %s%n", DATE.format(v.publishedAt()));
        out.printf("Fetched:      %s%n", DATE.format(v.fetchedAt()));
        out.printf("URL:          %s%n", v.url());
        if (v.description() != null && !v.description().isBlank()) {
            out.println();
            out.println("Description:");
            out.println("  " + v.description());
        }
        if (v.requirements() != null && !v.requirements().isBlank()) {
            out.println();
            out.println("Requirements:");
            out.println("  " + v.requirements());
        }
    }

    public static String formatSalary(Salary s) {
        if (s == null || s.isEmpty()) {
            return "salary not specified";
        }
        StringBuilder sb = new StringBuilder();
        if (s.from() != null && s.to() != null) {
            sb.append(s.from()).append("–").append(s.to());
        } else if (s.from() != null) {
            sb.append("from ").append(s.from());
        } else {
            sb.append("up to ").append(s.to());
        }
        if (s.currency() != null) {
            sb.append(" ").append(s.currency());
        }
        return sb.toString();
    }

    private static String nvl(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }
}
