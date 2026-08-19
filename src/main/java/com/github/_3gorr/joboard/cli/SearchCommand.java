package com.github._3gorr.joboard.cli;

import com.github._3gorr.joboard.model.SearchFilter;
import com.github._3gorr.joboard.service.SearchService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(name = "search", mixinStandardHelpOptions = true, description = "Full-text search in title / description / requirements.")
public final class SearchCommand implements Callable<Integer> {

    private final SearchService searchService;

    @Parameters(index = "0", description = "Query text", arity = "1")
    String query;

    @Option(names = "--city", description = "Restrict to city")
    String city;

    @Option(names = "--source", description = "Source code (hh, habr_career)")
    String source;

    @Option(names = "--min-salary")
    Integer minSalary;

    @Option(names = "--sort", defaultValue = "DATE_DESC")
    SearchFilter.SortBy sort;

    @Option(names = {"-n", "--limit"}, defaultValue = "20")
    int limit;

    public SearchCommand(SearchService searchService) {
        this.searchService = searchService;
    }

    @Override
    public Integer call() {
        SearchFilter filter = SearchFilter.builder()
                .query(query)
                .city(city)
                .sourceCode(source)
                .minSalary(minSalary)
                .sortBy(sort)
                .limit(limit)
                .build();
        VacancyPrinter.printList(searchService.search(filter), System.out);
        return 0;
    }
}
