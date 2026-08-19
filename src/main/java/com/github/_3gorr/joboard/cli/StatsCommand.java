package com.github._3gorr.joboard.cli;

import com.github._3gorr.joboard.service.StatsReport;
import com.github._3gorr.joboard.service.StatsService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "stats", mixinStandardHelpOptions = true, description = "Aggregate statistics over the stored vacancies.")
public final class StatsCommand implements Callable<Integer> {

    public enum Mode { CITY, COMPANY, SOURCE, SALARY }

    private final StatsService statsService;

    @Option(names = "--by", description = "${COMPLETION-CANDIDATES}")
    Mode by;

    @Option(names = "--salary", description = "Show salary aggregates")
    boolean salary;

    @Option(names = "--query", description = "Title substring filter for --salary")
    String query;

    @Option(names = {"-n", "--limit"}, defaultValue = "10",
            description = "Top N rows for --by (default: ${DEFAULT-VALUE})")
    int limit;

    public StatsCommand(StatsService statsService) {
        this.statsService = statsService;
    }

    @Override
    public Integer call() {
        if (salary || by == Mode.SALARY) {
            StatsReport.SalaryStats s = statsService.salaryStats(query);
            String title = query == null ? "all vacancies" : "title matches '" + query + "'";
            System.out.printf("Salary stats (%s):%n", title);
            if (s.countWithSalary() == 0) {
                System.out.println("  no vacancies with salary information");
                return 0;
            }
            System.out.printf("  count: %d%n", s.countWithSalary());
            System.out.printf("  min:   %s%n", s.min());
            System.out.printf("  max:   %s%n", s.max());
            System.out.printf("  avg:   %s%n", s.avg());
            return 0;
        }
        if (by == null) {
            System.err.println("Specify --by city|company|source or --salary");
            return 2;
        }
        List<StatsReport.CategoryCount> rows = switch (by) {
            case CITY -> statsService.byCity(limit);
            case COMPANY -> statsService.byCompany(limit);
            case SOURCE -> statsService.bySource();
            case SALARY -> List.of();
        };
        System.out.printf("%-30s %s%n", by.name().toLowerCase(), "count");
        for (StatsReport.CategoryCount cc : rows) {
            System.out.printf("%-30s %d%n", cc.label(), cc.count());
        }
        return 0;
    }
}
