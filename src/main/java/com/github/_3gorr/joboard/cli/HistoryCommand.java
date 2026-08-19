package com.github._3gorr.joboard.cli;

import com.github._3gorr.joboard.model.VacancyHistoryEntry;
import com.github._3gorr.joboard.repository.VacancyHistoryRepository;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "history", mixinStandardHelpOptions = true, description = "Show recent change history (insert / update / delete).")
public final class HistoryCommand implements Callable<Integer> {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final VacancyHistoryRepository historyRepository;

    @Option(names = {"-n", "--limit"}, defaultValue = "20")
    int limit;

    public HistoryCommand(VacancyHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    @Override
    public Integer call() {
        List<VacancyHistoryEntry> recent = historyRepository.recent(limit);
        if (recent.isEmpty()) {
            System.out.println("(no history yet)");
            return 0;
        }
        System.out.printf("%-20s %-8s %-15s %-12s %s%n",
                "WHEN", "OP", "SOURCE-ID", "VACANCY-ID", "EXTERNAL-ID");
        for (VacancyHistoryEntry e : recent) {
            System.out.printf("%-20s %-8s %-15d %-12s %s%n",
                    TS.format(e.occurredAt()),
                    e.operation(),
                    e.sourceId(),
                    e.vacancyId() == null ? "—" : e.vacancyId(),
                    e.externalId());
        }
        return 0;
    }
}
