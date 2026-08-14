package com.biodex.dao;

/**
 * Thrown when a database operation fails. Unchecked, so DAO methods stay readable and controllers
 * only catch it where they can actually show the user something useful.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(String message) {
        super(message);
    }
}
