package com.github._3gorr.joboard.repository;

import com.github._3gorr.joboard.model.VacancyHistoryEntry;
import com.github._3gorr.joboard.model.VacancyHistoryEntry.Operation;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class JdbcVacancyHistoryRepository implements VacancyHistoryRepository {

    private final DataSource dataSource;
    private final Clock clock;

    public JdbcVacancyHistoryRepository(DataSource dataSource, Clock clock) {
        this.dataSource = dataSource;
        this.clock = clock;
    }

    @Override
    public void log(Long vacancyId, long sourceId, String externalId,
                    VacancyHistoryEntry.Operation operation) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                     INSERT INTO vacancy_history(vacancy_id, source_id, external_id, operation, occurred_at)
                     VALUES (?,?,?,?,?)
                     """)) {
            if (vacancyId == null) ps.setNull(1, Types.INTEGER);
            else ps.setLong(1, vacancyId);
            ps.setLong(2, sourceId);
            ps.setString(3, externalId);
            ps.setString(4, operation.name());
            ps.setString(5, Instant.now(clock).toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("History log failed", e);
        }
    }

    @Override
    public List<VacancyHistoryEntry> recent(int limit) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                     SELECT id, vacancy_id, source_id, external_id, operation, occurred_at
                     FROM vacancy_history ORDER BY occurred_at DESC LIMIT ?
                     """)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<VacancyHistoryEntry> out = new ArrayList<>();
                while (rs.next()) {
                    long id = rs.getLong("id");
                    long vacancyIdRaw = rs.getLong("vacancy_id");
                    Long vacancyId = rs.wasNull() ? null : vacancyIdRaw;
                    long sourceId = rs.getLong("source_id");
                    String externalId = rs.getString("external_id");
                    Operation op = Operation.valueOf(rs.getString("operation"));
                    Instant when = Instant.parse(rs.getString("occurred_at"));
                    out.add(new VacancyHistoryEntry(id, vacancyId, sourceId, externalId, op, when));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RepositoryException("History query failed", e);
        }
    }
}
