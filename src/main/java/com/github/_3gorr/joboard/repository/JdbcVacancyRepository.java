package com.github._3gorr.joboard.repository;

import com.github._3gorr.joboard.model.Salary;
import com.github._3gorr.joboard.model.SearchFilter;
import com.github._3gorr.joboard.model.Vacancy;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcVacancyRepository implements VacancyRepository {

    private static final String COLUMNS = """
            v.id, v.source_id, v.external_id, v.url, v.title, v.company, v.city,
            v.salary_from, v.salary_to, v.salary_currency, v.employment_type,
            v.description, v.requirements, v.published_at, v.fetched_at, v.content_hash
            """;

    private final DataSource dataSource;

    public JdbcVacancyRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<Vacancy> findById(long id) {
        return querySingle("SELECT " + COLUMNS + " FROM vacancy v WHERE v.id = ?",
                ps -> ps.setLong(1, id));
    }

    @Override
    public Optional<Vacancy> findByExternalId(long sourceId, String externalId) {
        return querySingle(
                "SELECT " + COLUMNS + " FROM vacancy v WHERE v.source_id = ? AND v.external_id = ?",
                ps -> {
                    ps.setLong(1, sourceId);
                    ps.setString(2, externalId);
                });
    }

    @Override
    public List<Vacancy> search(SearchFilter filter) {
        StringBuilder sql = new StringBuilder("SELECT ").append(COLUMNS)
                .append(" FROM vacancy v");
        List<Object> params = new ArrayList<>();
        List<String> where = new ArrayList<>();

        filter.sourceCode().ifPresent(code -> {
            sql.append(" JOIN source s ON s.id = v.source_id");
            where.add("s.code = ?");
            params.add(code);
        });
        filter.city().ifPresent(city -> {
            where.add("v.city = ?");
            params.add(city);
        });
        filter.company().ifPresent(company -> {
            where.add("v.company = ?");
            params.add(company);
        });
        filter.minSalary().ifPresent(min -> {
            where.add("(v.salary_from >= ? OR v.salary_to >= ?)");
            params.add(min);
            params.add(min);
        });
        filter.maxSalary().ifPresent(max -> {
            where.add("(v.salary_from <= ? OR (v.salary_from IS NULL AND v.salary_to <= ?))");
            params.add(max);
            params.add(max);
        });
        filter.query().ifPresent(q -> {
            where.add("(LOWER(v.title) LIKE ? OR LOWER(v.description) LIKE ? OR LOWER(v.requirements) LIKE ?)");
            String like = "%" + q.toLowerCase() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        });
        filter.publishedAfter().ifPresent(after -> {
            where.add("v.published_at >= ?");
            params.add(after.toString());
        });
        filter.publishedBefore().ifPresent(before -> {
            where.add("v.published_at <= ?");
            params.add(before.toString());
        });

        if (!where.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", where));
        }
        sql.append(" ORDER BY ").append(orderClause(filter.sortBy()));
        sql.append(" LIMIT ?");
        params.add(filter.limit());

        return queryList(sql.toString(), ps -> {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
        });
    }

    @Override
    public long count() {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM vacancy");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new RepositoryException("Count failed", e);
        }
    }

    @Override
    public long insert(Vacancy v) {
        String sql = """
                INSERT INTO vacancy(source_id, external_id, url, title, company, city,
                    salary_from, salary_to, salary_currency, employment_type,
                    description, requirements, published_at, fetched_at, content_hash)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindVacancyFields(ps, v);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new RepositoryException("No generated key returned for vacancy insert", null);
            }
        } catch (SQLException e) {
            throw new RepositoryException("Insert vacancy failed", e);
        }
    }

    @Override
    public void update(Vacancy v) {
        if (v.id() == null) {
            throw new IllegalArgumentException("Vacancy id must be set for update");
        }
        String sql = """
                UPDATE vacancy SET
                    url = ?, title = ?, company = ?, city = ?,
                    salary_from = ?, salary_to = ?, salary_currency = ?, employment_type = ?,
                    description = ?, requirements = ?, published_at = ?, fetched_at = ?,
                    content_hash = ?
                WHERE id = ?
                """;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            Salary s = v.salary() == null ? Salary.none() : v.salary();
            ps.setString(1, v.url());
            ps.setString(2, v.title());
            setNullable(ps, 3, v.company());
            setNullable(ps, 4, v.city());
            setNullableInt(ps, 5, s.from());
            setNullableInt(ps, 6, s.to());
            setNullable(ps, 7, s.currency());
            setNullable(ps, 8, v.employmentType());
            setNullable(ps, 9, v.description());
            setNullable(ps, 10, v.requirements());
            ps.setString(11, v.publishedAt().toString());
            ps.setString(12, v.fetchedAt().toString());
            ps.setString(13, v.contentHash());
            ps.setLong(14, v.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Update vacancy failed", e);
        }
    }

    @Override
    public int deletePublishedBefore(Instant cutoff) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM vacancy WHERE published_at < ?")) {
            ps.setString(1, cutoff.toString());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Delete by cutoff failed", e);
        }
    }

    private void bindVacancyFields(PreparedStatement ps, Vacancy v) throws SQLException {
        Salary s = v.salary() == null ? Salary.none() : v.salary();
        ps.setLong(1, v.sourceId());
        ps.setString(2, v.externalId());
        ps.setString(3, v.url());
        ps.setString(4, v.title());
        setNullable(ps, 5, v.company());
        setNullable(ps, 6, v.city());
        setNullableInt(ps, 7, s.from());
        setNullableInt(ps, 8, s.to());
        setNullable(ps, 9, s.currency());
        setNullable(ps, 10, v.employmentType());
        setNullable(ps, 11, v.description());
        setNullable(ps, 12, v.requirements());
        ps.setString(13, v.publishedAt().toString());
        ps.setString(14, v.fetchedAt().toString());
        ps.setString(15, v.contentHash());
    }

    private Optional<Vacancy> querySingle(String sql, SqlBinder binder) {
        List<Vacancy> rs = queryList(sql, binder);
        return rs.isEmpty() ? Optional.empty() : Optional.of(rs.get(0));
    }

    private List<Vacancy> queryList(String sql, SqlBinder binder) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<Vacancy> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(map(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Vacancy query failed", e);
        }
    }

    private static Vacancy map(ResultSet rs) throws SQLException {
        Salary salary = new Salary(
                getNullableInt(rs, "salary_from"),
                getNullableInt(rs, "salary_to"),
                rs.getString("salary_currency"));
        return new Vacancy(
                rs.getLong("id"),
                rs.getLong("source_id"),
                rs.getString("external_id"),
                rs.getString("url"),
                rs.getString("title"),
                rs.getString("company"),
                rs.getString("city"),
                salary,
                rs.getString("employment_type"),
                rs.getString("description"),
                rs.getString("requirements"),
                Instant.parse(rs.getString("published_at")),
                Instant.parse(rs.getString("fetched_at")),
                rs.getString("content_hash"));
    }

    private static void setNullable(PreparedStatement ps, int idx, String v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.VARCHAR);
        else ps.setString(idx, v);
    }

    private static void setNullableInt(PreparedStatement ps, int idx, Integer v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.INTEGER);
        else ps.setInt(idx, v);
    }

    private static Integer getNullableInt(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    private static String orderClause(SearchFilter.SortBy sortBy) {
        return switch (sortBy) {
            case DATE_DESC -> "v.published_at DESC";
            case DATE_ASC -> "v.published_at ASC";
            case SALARY_DESC -> "COALESCE(v.salary_to, v.salary_from, 0) DESC";
            case SALARY_ASC -> "COALESCE(v.salary_from, v.salary_to, 0) ASC";
            case COMPANY_ASC -> "v.company ASC";
        };
    }

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}
