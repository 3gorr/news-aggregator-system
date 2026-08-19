package com.github._3gorr.joboard.cli;

import com.github._3gorr.joboard.model.Vacancy;
import com.github._3gorr.joboard.service.SearchService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.Optional;
import java.util.concurrent.Callable;

@Command(name = "show", mixinStandardHelpOptions = true, description = "Show full details of a single vacancy by its local ID.")
public final class ShowCommand implements Callable<Integer> {

    private final SearchService searchService;

    @Parameters(index = "0", description = "Vacancy ID")
    long id;

    public ShowCommand(SearchService searchService) {
        this.searchService = searchService;
    }

    @Override
    public Integer call() {
        Optional<Vacancy> v = searchService.findById(id);
        if (v.isEmpty()) {
            System.err.println("Vacancy " + id + " not found");
            return 1;
        }
        VacancyPrinter.printDetail(v.get(), System.out);
        return 0;
    }
}
