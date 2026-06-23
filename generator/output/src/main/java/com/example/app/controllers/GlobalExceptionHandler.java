package com.example.app.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;
import jakarta.validation.ConstraintViolationException;
import org.springframework.transaction.TransactionSystemException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleValidationError(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
    }

    // violações de pré/pós-condição (OCL) são erros do cliente -> 400, não 404
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleContractError(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
    }

    // violações Bean Validation (anotações OCL) -> 400. Vêm diretas (insert imediato
    // com @GeneratedValue IDENTITY) OU embrulhadas em TransactionSystemException (commit/jobs)
    @ExceptionHandler({ConstraintViolationException.class, TransactionSystemException.class})
    public ResponseEntity<Map<String, String>> handleValidation(Exception ex) {
        Throwable cause = (ex instanceof TransactionSystemException tse) ? tse.getRootCause() : ex;
        if (cause instanceof ConstraintViolationException cve) {
            String msg = cve.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .collect(java.util.stream.Collectors.joining(", "));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", msg));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
    }
}
