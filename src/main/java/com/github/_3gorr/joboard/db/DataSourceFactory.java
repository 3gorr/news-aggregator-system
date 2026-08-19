package com.github._3gorr.joboard.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

public final class DataSourceFactory {

    private DataSourceFactory() {
    }

    public static DataSource create(AppConfig config) {
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(config.dbUrl());
        hikari.setMaximumPoolSize(config.dbPoolSize());
        hikari.setPoolName("joboard-pool");
        hikari.addDataSourceProperty("foreign_keys", "true");
        HikariDataSource ds = new HikariDataSource(hikari);
        migrate(ds);
        return ds;
    }

    private static void migrate(DataSource ds) {
        Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }
}
