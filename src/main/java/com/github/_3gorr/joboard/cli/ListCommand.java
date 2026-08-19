package com.github._3gorr.joboard.cli;

import com.github._3gorr.joboard.model.SearchFilter;
import com.github._3gorr.joboard.service.SearchService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(name = "list", mixinStandardHelpOptions = true, description = "List stored vacancies, optionally filtered.")
public final class ListCommand implements Callable<Integer> {

    private final SearchService searchService;

    @Option(names = "--city", description = "Exact city match (case-sensitive)")
    String city;

    @Option(names = "--company", description = "Exact company match")
    String company;

    @Option(names = "--source", description = "Source code (hh, habr_career)")
    String source;

    @Option(names = "--min-salary", description = "Lower salary bound")
    Integer minSalary;

    @Option(names = "--max-salary", description = "Upper salary bound")
    Integer maxSalary;

    @Option(names = "--sort", defaultValue = "DATE_DESC",
            description = "Sort by: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})")
    SearchFilter.SortBy sort;

    @Option(names = {"-n", "--limit"}, defaultValue = "20", description = "Max results (default: ${DEFAULT-VALUE})")
    int limit;

    public ListCommand(SearchService searchService) {
        this.searchService = searchService;
    }

    @Override
    public Integer call() {
        SearchFilter filter = SearchFilter.builder()
                .city(city)
                .company(company)
                .sourceCode(source)
                .minSalary(minSalary)
                .maxSalary(maxSalary)
                .sortBy(sort)
                .limit(limit)
                .build();
        VacancyPrinter.printList(searchService.search(filter), System.out);
        return 0;
    }
}
