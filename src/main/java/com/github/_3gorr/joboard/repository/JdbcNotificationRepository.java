package com.github._3gorr.joboard.repository;

import com.github._3gorr.joboard.model.NotificationFilter;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcNotificationRepository implements NotificationRepository {

    private final DataSource dataSource;
    private final Clock clock;

    public JdbcNotificationRepository(DataSource dataSource, Clock clock) {
        this.dataSource = dataSource;
        this.clock = clock;
    }

    @Override
    public long add(String name, String query, String city, Integer minSalary) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                     INSERT INTO notification_filter(name, query, city, min_salary, created_at)
                     VALUES (?,?,?,?,?)
                     """, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            setNullableString(ps, 2, query);
            setNullableString(ps, 3, city);
            if (minSalary == null) ps.setNull(4, Types.INTEGER);
            else ps.setInt(4, minSalary);
            ps.setString(5, Instant.now(clock).toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
                throw new RepositoryException("No id generated for notification filter", null);
            }
        } catch (SQLException e) {
            throw new RepositoryException("Add notification filter failed", e);
        }
    }

    @Override
    public List<NotificationFilter> findAll() {
        return query("""
                SELECT id, name, query, city, min_salary, created_at
                FROM notification_filter ORDER BY name
                """, ps -> {});
    }

    @Override
    public Optional<NotificationFilter> findByName(String name) {
        List<NotificationFilter> r = query("""
                SELECT id, name, query, city, min_salary, created_at
                FROM notification_filter WHERE name = ?
                """, ps -> ps.setString(1, name));
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    @Override
    public boolean removeByName(String name) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM notification_filter WHERE name = ?")) {
            ps.setString(1, name);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositoryException("Remove notification filter failed", e);
        }
    }

    private List<NotificationFilter> query(String sql, SqlBinder binder) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<NotificationFilter> out = new ArrayList<>();
                while (rs.next()) {
                    int ms = rs.getInt("min_salary");
                    Integer minSalary = rs.wasNull() ? null : ms;
                    out.add(new NotificationFilter(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getString("query"),
                            rs.getString("city"),
                            minSalary,
                            Instant.parse(rs.getString("created_at"))));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Notification query failed", e);
        }
    }

    private static void setNullableString(PreparedStatement ps, int idx, String v) throws SQLException {
        if (v == null) ps.setNull(idx, Types.VARCHAR);
        else ps.setString(idx, v);
    }

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}
