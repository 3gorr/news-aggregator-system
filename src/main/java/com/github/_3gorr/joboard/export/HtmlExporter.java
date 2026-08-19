package com.github._3gorr.joboard.export;

import com.github._3gorr.joboard.cli.VacancyPrinter;
import com.github._3gorr.joboard.model.Vacancy;

import java.io.IOException;
import java.io.Writer;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class HtmlExporter implements Exporter {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault());

    @Override
    public String format() {
        return "html";
    }

    @Override
    public void write(List<Vacancy> vacancies, Writer out) throws IOException {
        out.write("""
                <!DOCTYPE html>
                <html lang="ru">
                <head>
                <meta charset="UTF-8">
                <title>joboard export</title>
                <style>
                  body { font-family: -apple-system, Helvetica, sans-serif; margin: 2em; }
                  table { border-collapse: collapse; width: 100%; }
                  th, td { border: 1px solid #ddd; padding: 8px; text-align: left; vertical-align: top; }
                  th { background: #f4f4f4; }
                  tr:nth-child(even) { background: #fafafa; }
                  .salary { white-space: nowrap; }
                  a { color: #1565c0; text-decoration: none; }
                  a:hover { text-decoration: underline; }
                </style>
                </head>
                <body>
                """);
        out.write("<h1>joboard export (" + vacancies.size() + " vacancies)</h1>\n");
        out.write("<table>\n<thead><tr>"
                + "<th>ID</th><th>Date</th><th>Title</th><th>Company</th>"
                + "<th>City</th><th class=\"salary\">Salary</th><th>Link</th>"
                + "</tr></thead>\n<tbody>\n");
        for (Vacancy v : vacancies) {
            out.write("<tr>");
            cell(out, v.id() == null ? "" : v.id().toString());
            cell(out, DATE.format(v.publishedAt()));
            cell(out, v.title());
            cell(out, v.company());
            cell(out, v.city());
            cell(out, VacancyPrinter.formatSalary(v.salary()));
            out.write("<td><a href=\"" + escape(v.url()) + "\">link</a></td>");
            out.write("</tr>\n");
        }
        out.write("</tbody>\n</table>\n</body>\n</html>\n");
        out.flush();
    }

    private static void cell(Writer out, String value) throws IOException {
        out.write("<td>" + escape(value == null ? "" : value) + "</td>");
    }

    private static String escape(String s) {
        return s
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
