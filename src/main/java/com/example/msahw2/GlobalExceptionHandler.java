package com.example.msahw2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(error.getField(), error.getDefaultMessage()));

        log.atWarn()
                .addKeyValue("error_type", "VALIDATION_ERROR")
                .addKeyValue("validation_errors", errors.toString())
                .log("Request validation failed");

        return ResponseEntity
                .badRequest()
                .body(errors);
    }

    @ExceptionHandler({
            DataAccessException.class,
            CannotCreateTransactionException.class
    })
    public ResponseEntity<Map<String, String>> handleDatabaseException(
            Exception ex) {

        log.atError()
                .addKeyValue("error_type", "DATABASE_ERROR")
                .setCause(ex)
                .log("Database operation failed");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "Database error"
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpectedException(
            Exception ex) {

        log.atError()
                .addKeyValue("error_type", "INTERNAL_ERROR")
                .setCause(ex)
                .log("Unexpected application error");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "Internal server error"
                ));
    }
}