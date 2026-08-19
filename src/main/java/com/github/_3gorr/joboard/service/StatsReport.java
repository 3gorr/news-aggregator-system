package com.github._3gorr.joboard.service;

import java.util.List;

public final class StatsReport {

    public record CategoryCount(String label, long count) {}

    public record SalaryStats(long countWithSalary, Integer min, Integer max, Long avg) {
        public static SalaryStats empty() {
            return new SalaryStats(0, null, null, null);
        }
    }

    public record Trend(String keyword, long count, long previousCount) {
        public long delta() {
            return count - previousCount;
        }
    }

    public static List<CategoryCount> emptyCounts() {
        return List.of();
    }
}
