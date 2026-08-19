package com.github._3gorr.joboard.cli;

import com.github._3gorr.joboard.service.FetchReport;
import com.github._3gorr.joboard.service.FetchService;
import com.github._3gorr.joboard.source.FetchQuery;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Map;
import java.util.concurrent.Callable;

@Command(name = "fetch", mixinStandardHelpOptions = true, description = "Fetch vacancies from configured sources.")
public final class FetchCommand implements Callable<Integer> {

    private final FetchService fetchService;

    @Option(names = "--source", description = "Source code (default: all enabled)")
    String source;

    @Option(names = {"-q", "--query"}, description = "Free-text query passed to the source")
    String query;

    @Option(names = "--pages", defaultValue = "1", description = "How many pages to fetch per source (default: ${DEFAULT-VALUE})")
    int pages;

    @Option(names = "--per-page", defaultValue = "20", description = "Page size hint (default: ${DEFAULT-VALUE})")
    int perPage;

    public FetchCommand(FetchService fetchService) {
        this.fetchService = fetchService;
    }

    @Override
    public Integer call() {
        FetchQuery q = FetchQuery.of(query, pages, perPage);
        FetchReport report = (source == null)
                ? fetchService.fetchAll(q)
                : fetchService.fetchOne(source, q);

        System.out.println("Fetch report:");
        for (Map.Entry<String, FetchReport.SourceStats> e : report.bySource().entrySet()) {
            FetchReport.SourceStats s = e.getValue();
            System.out.printf("  %-12s seen=%d  +%d (new)  ~%d (updated)  =%d (unchanged)  !%d (failed)%n",
                    e.getKey(), s.seen(), s.inserted(), s.updated(), s.unchanged(), s.failed());
        }
        System.out.printf("Total: +%d  ~%d  =%d  !%d%n",
                report.totalInserted(), report.totalUpdated(),
                report.totalUnchanged(), report.totalFailed());
        return 0;
    }
}
