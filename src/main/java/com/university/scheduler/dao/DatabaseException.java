package com.university.scheduler.dao;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DatabaseException extends RuntimeException {
    private static final Logger LOG=LogManager.getLogger(DatabaseException.class);
    public DatabaseException(String message, Throwable cause) { super(message, cause); LOG.error(message,cause); }
}
