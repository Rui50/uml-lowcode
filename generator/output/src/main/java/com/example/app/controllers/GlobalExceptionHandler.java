package com.example.app.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.validation.ConstraintViolationException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.dao.DataIntegrityViolationException;
import com.example.app.exception.NotFoundException;

// Respostas de erro em formato ProblemDetail (RFC 7807/9457). Ref:
// https://www.baeldung.com/spring-boot-return-errors-problemdetail
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleValidationError(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, msg(ex));
    }

    // violações de pré/pós-condição (OCL) são erros do cliente -> 400, não 404
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleContractError(IllegalStateException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, msg(ex));
    }

    // violações Bean Validation (anotações OCL) -> 400. Vêm diretas (insert imediato
    // com @GeneratedValue IDENTITY) OU embrulhadas em TransactionSystemException (commit/jobs)
    @ExceptionHandler({ConstraintViolationException.class, TransactionSystemException.class})
    public ProblemDetail handleValidation(Exception ex) {
        Throwable cause = (ex instanceof TransactionSystemException tse) ? tse.getRootCause() : ex;
        if (cause instanceof ConstraintViolationException cve) {
            String detail = cve.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .collect(java.util.stream.Collectors.joining(", "));
            return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        }
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, msg(ex));
    }

    // violação de integridade referencial (ex: apagar entidade ainda
    // referenciada por outra, ou valor único duplicado) -> 409. NÃO expõe
    // ex.getMessage() porque traz SQL/constraint cru (info leak).
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegrity(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
            "Data integrity violation: the resource is still referenced by other records or violates a constraint.");
    }

    // entidade não encontrada -> 404
    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, msg(ex));
    }

    // qualquer outro erro inesperado do servidor -> 500 (NÃO 404)
    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail handleServerError(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, msg(ex));
    }

    // mensagem do erro, ou o nome da exceção se não houver mensagem
    private static String msg(Exception ex) {
        return ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
    }
}
