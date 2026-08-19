package com.github._3gorr.joboard;

import com.github._3gorr.joboard.db.AppConfig;
import com.github._3gorr.joboard.db.DataSourceFactory;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TestDatabase implements AutoCloseable {

    private final Path file;
    private final DataSource dataSource;

    private TestDatabase(Path file, DataSource dataSource) {
        this.file = file;
        this.dataSource = dataSource;
    }

    public static TestDatabase create() {
        try {
            Path tmp = Files.createTempFile("joboard-test-", ".db");
            Files.deleteIfExists(tmp);
            AppConfig cfg = AppConfig.forTest("jdbc:sqlite:" + tmp.toAbsolutePath());
            DataSource ds = DataSourceFactory.create(cfg);
            return new TestDatabase(tmp, ds);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create test DB", e);
        }
    }

    public DataSource dataSource() {
        return dataSource;
    }

    @Override
    public void close() {
        if (dataSource instanceof HikariDataSource hds) {
            hds.close();
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
        }
    }
}
