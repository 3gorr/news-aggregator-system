package com.github._3gorr.joboard.model;

import java.time.Instant;
import java.util.Optional;

public record SearchFilter(
        Optional<String> query,
        Optional<String> city,
        Optional<String> company,
        Optional<String> sourceCode,
        Optional<Integer> minSalary,
        Optional<Integer> maxSalary,
        Optional<Instant> publishedAfter,
        Optional<Instant> publishedBefore,
        SortBy sortBy,
        int limit
) {

    public enum SortBy {
        DATE_DESC, DATE_ASC, SALARY_DESC, SALARY_ASC, COMPANY_ASC
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String query;
        private String city;
        private String company;
        private String sourceCode;
        private Integer minSalary;
        private Integer maxSalary;
        private Instant publishedAfter;
        private Instant publishedBefore;
        private SortBy sortBy = SortBy.DATE_DESC;
        private int limit = 50;

        public Builder query(String v) { this.query = v; return this; }
        public Builder city(String v) { this.city = v; return this; }
        public Builder company(String v) { this.company = v; return this; }
        public Builder sourceCode(String v) { this.sourceCode = v; return this; }
        public Builder minSalary(Integer v) { this.minSalary = v; return this; }
        public Builder maxSalary(Integer v) { this.maxSalary = v; return this; }
        public Builder publishedAfter(Instant v) { this.publishedAfter = v; return this; }
        public Builder publishedBefore(Instant v) { this.publishedBefore = v; return this; }
        public Builder sortBy(SortBy v) { this.sortBy = v; return this; }
        public Builder limit(int v) { this.limit = v; return this; }

        public SearchFilter build() {
            return new SearchFilter(
                    Optional.ofNullable(query),
                    Optional.ofNullable(city),
                    Optional.ofNullable(company),
                    Optional.ofNullable(sourceCode),
                    Optional.ofNullable(minSalary),
                    Optional.ofNullable(maxSalary),
                    Optional.ofNullable(publishedAfter),
                    Optional.ofNullable(publishedBefore),
                    sortBy,
                    limit
            );
        }
    }
}
