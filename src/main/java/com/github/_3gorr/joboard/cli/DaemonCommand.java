package com.github._3gorr.joboard.cli;

import com.github._3gorr.joboard.service.FetchService;
import com.github._3gorr.joboard.service.SchedulerService;
import com.github._3gorr.joboard.source.FetchQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Callable;

@Command(name = "daemon", mixinStandardHelpOptions = true, description = "Run periodic background fetch until interrupted (Ctrl+C).")
public final class DaemonCommand implements Callable<Integer> {

    private static final Logger LOG = LoggerFactory.getLogger(DaemonCommand.class);

    private final FetchService fetchService;
    private final SchedulerService scheduler;

    @Option(names = "--interval", defaultValue = "1h",
            description = "Fetch interval (e.g. 30s, 5m, 1h, default: ${DEFAULT-VALUE})")
    String interval;

    @Option(names = "--source", description = "Restrict fetch to one source code")
    String source;

    @Option(names = {"-q", "--query"}, description = "Free-text query passed to source")
    String query;

    @Option(names = "--pages", defaultValue = "1") int pages;
    @Option(names = "--per-page", defaultValue = "20") int perPage;

    public DaemonCommand(FetchService fetchService, SchedulerService scheduler) {
        this.fetchService = fetchService;
        this.scheduler = scheduler;
    }

    @Override
    public Integer call() throws InterruptedException {
        Duration period = DurationParser.parse(interval);
        FetchQuery q = FetchQuery.of(query, pages, perPage);
        CountDownLatch stop = new CountDownLatch(1);

        Runnable task = () -> {
            LOG.info("Daemon tick: fetching...");
            try {
                if (source == null) fetchService.fetchAll(q);
                else fetchService.fetchOne(source, q);
            } catch (RuntimeException e) {
                LOG.error("Daemon fetch failed: {}", e.getMessage(), e);
            }
        };

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println();
            LOG.info("Shutting down daemon...");
            scheduler.close();
            stop.countDown();
        }, "joboard-shutdown"));

        scheduler.start(period, task);
        System.err.printf("Daemon started, interval %s. Press Ctrl+C to stop.%n", period);
        stop.await();
        return 0;
    }
}
