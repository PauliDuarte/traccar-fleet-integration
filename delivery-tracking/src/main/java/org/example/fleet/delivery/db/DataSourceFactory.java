package org.example.fleet.delivery.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public final class DataSourceFactory {
    private DataSourceFactory() {
    }

    public static DataSource fromEnvironment() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(required("DELIVERY_DB_URL"));
        config.setUsername(required("POSTGRES_USER"));
        config.setPassword(required("POSTGRES_PASSWORD"));
        config.setMaximumPoolSize(5);
        config.setPoolName("delivery-postgres");
        return new HikariDataSource(config);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value;
    }
}
