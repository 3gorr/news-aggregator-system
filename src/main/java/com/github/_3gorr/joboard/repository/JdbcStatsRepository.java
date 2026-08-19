package com.github._3gorr.joboard.repository;

import com.github._3gorr.joboard.service.StatsReport;
import com.github._3gorr.joboard.service.StatsReport.CategoryCount;
import com.github._3gorr.joboard.service.StatsReport.SalaryStats;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class JdbcStatsRepository implements StatsRepository {

    private final DataSource dataSource;

    public JdbcStatsRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<CategoryCount> countByCity(int limit) {
        return groupCount("""
                SELECT COALESCE(city, '(unknown)') AS label, COUNT(*) AS cnt
                FROM vacancy GROUP BY label ORDER BY cnt DESC LIMIT ?
                """, limit);
    }

    @Override
    public List<CategoryCount> countByCompany(int limit) {
        return groupCount("""
                SELECT COALESCE(company, '(unknown)') AS label, COUNT(*) AS cnt
                FROM vacancy GROUP BY label ORDER BY cnt DESC LIMIT ?
                """, limit);
    }

    @Override
    public List<CategoryCount> countBySource() {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                     SELECT s.code AS label, COUNT(v.id) AS cnt
                     FROM source s LEFT JOIN vacancy v ON v.source_id = s.id
                     GROUP BY s.code ORDER BY cnt DESC
                     """);
             ResultSet rs = ps.executeQuery()) {
            List<CategoryCount> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new CategoryCount(rs.getString("label"), rs.getLong("cnt")));
            }
            return out;
        } catch (SQLException e) {
            throw new RepositoryException("countBySource failed", e);
        }
    }

    @Override
    public SalaryStats salaryStats(String titleQuery) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) AS cnt,
                       MIN(COALESCE(salary_from, salary_to)) AS min_s,
                       MAX(COALESCE(salary_to, salary_from)) AS max_s,
                       AVG(COALESCE((salary_from + salary_to) / 2.0, salary_from, salary_to)) AS avg_s
                FROM vacancy
                WHERE (salary_from IS NOT NULL OR salary_to IS NOT NULL)
                """);
        boolean hasQuery = titleQuery != null && !titleQuery.isBlank();
        if (hasQuery) {
            sql.append(" AND LOWER(title) LIKE ?");
        }
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            if (hasQuery) {
                ps.setString(1, "%" + titleQuery.toLowerCase() + "%");
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return SalaryStats.empty();
                long cnt = rs.getLong("cnt");
                if (cnt == 0) return SalaryStats.empty();
                int min = rs.getInt("min_s");
                Integer minOrNull = rs.wasNull() ? null : min;
                int max = rs.getInt("max_s");
                Integer maxOrNull = rs.wasNull() ? null : max;
                double avg = rs.getDouble("avg_s");
                Long avgOrNull = rs.wasNull() ? null : Math.round(avg);
                return new SalaryStats(cnt, minOrNull, maxOrNull, avgOrNull);
            }
        } catch (SQLException e) {
            throw new RepositoryException("salaryStats failed", e);
        }
    }

    private List<CategoryCount> groupCount(String sql, int limit) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<CategoryCount> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new CategoryCount(rs.getString("label"), rs.getLong("cnt")));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RepositoryException("group count failed", e);
        }
    }
}
