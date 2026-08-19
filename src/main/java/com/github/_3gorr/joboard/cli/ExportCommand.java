package com.github._3gorr.joboard.cli;

import com.github._3gorr.joboard.export.Exporter;
import com.github._3gorr.joboard.export.ExporterFactory;
import com.github._3gorr.joboard.model.SearchFilter;
import com.github._3gorr.joboard.model.Vacancy;
import com.github._3gorr.joboard.service.SearchService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "export", mixinStandardHelpOptions = true, description = "Export filtered vacancies to CSV / JSON / HTML.")
public final class ExportCommand implements Callable<Integer> {

    private final SearchService searchService;
    private final ExporterFactory exporters;

    @Option(names = "--format", required = true, description = "csv | json | html")
    String format;

    @Option(names = {"-o", "--out"}, description = "Output file (default: stdout)")
    Path out;

    @Option(names = "--city") String city;
    @Option(names = "--company") String company;
    @Option(names = "--source") String source;
    @Option(names = "--min-salary") Integer minSalary;
    @Option(names = "--max-salary") Integer maxSalary;
    @Option(names = "--query") String query;

    @Option(names = "--sort", defaultValue = "DATE_DESC") SearchFilter.SortBy sort;
    @Option(names = {"-n", "--limit"}, defaultValue = "10000") int limit;

    public ExportCommand(SearchService searchService, ExporterFactory exporters) {
        this.searchService = searchService;
        this.exporters = exporters;
    }

    @Override
    public Integer call() throws IOException {
        Exporter exporter = exporters.get(format);
        SearchFilter filter = SearchFilter.builder()
                .query(query).city(city).company(company).sourceCode(source)
                .minSalary(minSalary).maxSalary(maxSalary)
                .sortBy(sort).limit(limit).build();
        List<Vacancy> data = searchService.search(filter);

        if (out == null) {
            try (Writer w = new BufferedWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8))) {
                exporter.write(data, w);
            }
        } else {
            try (Writer w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
                exporter.write(data, w);
            }
            System.err.printf("Exported %d vacancies to %s%n", data.size(), out);
        }
        return 0;
    }
}
