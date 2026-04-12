package com.eaglebank.cbs.core_engine.exception;

import com.eaglebank.cbs.core_engine.model.BadRequestErrorResponse;
import com.eaglebank.cbs.core_engine.model.BadRequestErrorResponseDetailsInner;
import com.eaglebank.cbs.core_engine.model.ErrorResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalApiExceptionHandler {

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

  private String resolveMessage(ResponseStatusException ex) {
    if (ex.getReason() != null && !ex.getReason().isBlank()) {
      return ex.getReason();
    }
    return "An unexpected error occurred";
  }
}

