package com.github._3gorr.joboard.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class SchedulerService implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SchedulerService.class);

    private final ScheduledExecutorService executor;
    private boolean started = false;

    public SchedulerService() {
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "joboard-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    public synchronized void start(Duration interval, Runnable task) {
        if (started) {
            throw new IllegalStateException("Scheduler already started");
        }
        if (interval.isNegative() || interval.isZero()) {
            throw new IllegalArgumentException("interval must be positive");
        }
        long ms = interval.toMillis();
        executor.scheduleAtFixedRate(() -> safeRun(task), ms, ms, TimeUnit.MILLISECONDS);
        started = true;
        LOG.info("Scheduler started: every {}", interval);
    }

    private void safeRun(Runnable task) {
        try {
            task.run();
        } catch (RuntimeException e) {
            LOG.error("Scheduled task failed: {}", e.getMessage(), e);
        }
    }

    @Override
    public synchronized void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
