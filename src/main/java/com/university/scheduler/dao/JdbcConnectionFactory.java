package com.university.scheduler.dao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Reads database settings from config.properties, with environment overrides. */
public final class JdbcConnectionFactory implements ConnectionProvider {
    private final String url;
    private final String username;
    private final String password;

    public JdbcConnectionFactory() { this(loadProperties()); }

    public JdbcConnectionFactory(Properties properties) {
        url = setting(properties, "db.url", "DB_URL", "jdbc:mysql://localhost:3306/scheduler_db");
        username = setting(properties, "db.username", "DB_USERNAME", "root");
        password = setting(properties, "db.password", "DB_PASSWORD", "");
        String driver = setting(properties, "db.driver", "DB_DRIVER", "com.mysql.cj.jdbc.Driver");
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new DatabaseException("JDBC driver not found: " + driver, e);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    private static String setting(Properties p, String key, String env, String fallback) {
        String value = System.getenv(env);
        if (value == null || value.isBlank()) value = p.getProperty(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static Properties loadProperties() {
        Properties p = new Properties();
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) p.load(in);
            Path local = Path.of("config.properties");
            if (Files.isRegularFile(local)) try (InputStream file = Files.newInputStream(local)) { p.load(file); }
            return p;
        } catch (IOException e) {
            throw new DatabaseException("Could not read database configuration", e);
        }
    }
}
