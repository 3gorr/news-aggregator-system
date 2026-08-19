package com.github._3gorr.joboard.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SchedulerServiceTest {

    @Test
    void runsTaskRepeatedlyAtFixedInterval() throws Exception {
        CountDownLatch ranAtLeast3Times = new CountDownLatch(3);
        AtomicInteger counter = new AtomicInteger();
        try (SchedulerService scheduler = new SchedulerService()) {
            scheduler.start(Duration.ofMillis(40), () -> {
                counter.incrementAndGet();
                ranAtLeast3Times.countDown();
            });
            assertThat(ranAtLeast3Times.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(counter.get()).isGreaterThanOrEqualTo(3);
        }
    }

    @Test
    void taskRuntimeExceptionDoesNotCancelFutureRuns() throws Exception {
        CountDownLatch latch = new CountDownLatch(3);
        try (SchedulerService scheduler = new SchedulerService()) {
            scheduler.start(Duration.ofMillis(30), () -> {
                latch.countDown();
                throw new RuntimeException("boom");
            });
            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void closeStopsFutureRuns() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        SchedulerService scheduler = new SchedulerService();
        scheduler.start(Duration.ofMillis(30), counter::incrementAndGet);
        Thread.sleep(120);
        scheduler.close();
        int countAtShutdown = counter.get();
        Thread.sleep(200);
        assertThat(counter.get()).isEqualTo(countAtShutdown);
    }

    @Test
    void rejectsZeroOrNegativeInterval() {
        try (SchedulerService scheduler = new SchedulerService()) {
            assertThatThrownBy(() -> scheduler.start(Duration.ZERO, () -> {}))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> scheduler.start(Duration.ofSeconds(-1), () -> {}))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void cannotStartTwice() {
        try (SchedulerService scheduler = new SchedulerService()) {
            scheduler.start(Duration.ofSeconds(10), () -> {});
            assertThatThrownBy(() -> scheduler.start(Duration.ofSeconds(10), () -> {}))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
