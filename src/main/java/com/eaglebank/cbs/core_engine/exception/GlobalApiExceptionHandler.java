package com.eaglebank.cbs.core_engine.exception;

import com.eaglebank.cbs.core_engine.model.BadRequestErrorResponse;
import com.eaglebank.cbs.core_engine.model.BadRequestErrorResponseDetailsInner;
import com.eaglebank.cbs.core_engine.model.ErrorResponse;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalApiExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<BadRequestErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex) {
    List<BadRequestErrorResponseDetailsInner> details = ex.getBindingResult()
        .getAllErrors()
        .stream()
        .map(error -> {
          String field = (error instanceof FieldError fe) ? fe.getField() : error.getObjectName();
          String message = error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value";
          return new BadRequestErrorResponseDetailsInner(field, message, "validation");
        })
        .collect(Collectors.toList());

    BadRequestErrorResponse body = new BadRequestErrorResponse("Validation failed", details);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Object> handleResponseStatusException(ResponseStatusException ex) {
    String message = resolveMessage(ex);

    if (ex.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()) {
      BadRequestErrorResponseDetailsInner detail =
          new BadRequestErrorResponseDetailsInner("request", message, "validation");
      BadRequestErrorResponse body = new BadRequestErrorResponse(message, List.of(detail));
      return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    return ResponseEntity.status(ex.getStatusCode()).body(new ErrorResponse(message));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
    String message = ex.getMessage() != null ? ex.getMessage() : "Access denied";
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(message));
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
    String message = ex.getMessage() != null ? ex.getMessage() : "Access token is missing or invalid";
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(message));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponse("An unexpected error occurred"));
  }

  private String resolveMessage(ResponseStatusException ex) {
    if (ex.getReason() != null && !ex.getReason().isBlank()) {
      return ex.getReason();
    }
    return "An unexpected error occurred";
  }
}

