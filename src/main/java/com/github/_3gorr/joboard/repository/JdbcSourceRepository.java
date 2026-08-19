package com.github._3gorr.joboard.repository;

import com.github._3gorr.joboard.model.Source;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcSourceRepository implements SourceRepository {

    private final DataSource dataSource;

    public JdbcSourceRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Source> findAll() {
        return query("SELECT id, code, name, base_url, enabled FROM source ORDER BY code", ps -> {});
    }

    @Override
    public List<Source> findEnabled() {
        return query("SELECT id, code, name, base_url, enabled FROM source WHERE enabled = 1 ORDER BY code",
                ps -> {});
    }

    @Override
    public Optional<Source> findByCode(String code) {
        List<Source> rs = query(
                "SELECT id, code, name, base_url, enabled FROM source WHERE code = ?",
                ps -> ps.setString(1, code));
        return rs.isEmpty() ? Optional.empty() : Optional.of(rs.get(0));
    }

    @Override
    public void setEnabled(String code, boolean enabled) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE source SET enabled = ? WHERE code = ?")) {
            ps.setInt(1, enabled ? 1 : 0);
            ps.setString(2, code);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new IllegalArgumentException("Unknown source code: " + code);
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to update source " + code, e);
        }
    }

    private List<Source> query(String sql, SqlBinder binder) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<Source> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(map(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Source query failed", e);
        }
    }

    private static Source map(ResultSet rs) throws SQLException {
        return new Source(
                rs.getLong("id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("base_url"),
                rs.getInt("enabled") == 1);
    }

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}
