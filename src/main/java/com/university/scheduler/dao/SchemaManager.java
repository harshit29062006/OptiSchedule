package com.university.scheduler.dao;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public final class SchemaManager {
    private static final Logger LOG = LogManager.getLogger(SchemaManager.class);
    private SchemaManager() {}

    public static void createSchema(ConnectionProvider provider) {
        try (InputStream in = SchemaManager.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (in == null) throw new FileNotFoundException("schema.sql is missing from the classpath");
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("(?m)^\\s*--.*$", "");
            try (Connection c = provider.getConnection(); Statement s = c.createStatement()) {
                for (String statement : sql.split(";")) if (!statement.isBlank()) s.execute(statement);
            }
            LOG.info("Database schema created or verified");
        } catch (IOException | SQLException e) {
            throw new DatabaseException("Could not create database schema", e);
        }
    }
}
