package com.university.scheduler.dao;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Central JDBC connection factory. Credentials may be overridden with environment variables. */
public final class Database {
    private static final Properties PROPERTIES = load();
    private Database() {}

    private static Properties load() {
        Properties p = new Properties();
        try (InputStream in = Database.class.getResourceAsStream("/config.properties")) {
            if (in != null) p.load(in);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot read /config.properties", e);
        }
        return p;
    }

    public static Connection getConnection() throws SQLException {
        String url = env("OPTISCHEDULE_DB_URL", PROPERTIES.getProperty("db.url", "jdbc:mysql://localhost:3306/scheduler_db"));
        String user = env("OPTISCHEDULE_DB_USERNAME", PROPERTIES.getProperty("db.username", "root"));
        String password = env("OPTISCHEDULE_DB_PASSWORD", PROPERTIES.getProperty("db.password", ""));
        String driver = PROPERTIES.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
        try { Class.forName(driver); }
        catch (ClassNotFoundException e) { throw new SQLException("MySQL JDBC driver is missing", e); }
        return DriverManager.getConnection(url, user, password);
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
