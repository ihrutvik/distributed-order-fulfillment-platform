package com.hrutvik.orders.api;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(OrderNotFoundException.class) ResponseEntity<Problem> notFound(OrderNotFoundException e){return ResponseEntity.status(404).body(new Problem("ORDER_NOT_FOUND",e.getMessage(),Instant.now()));}
  @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<Problem> invalid(MethodArgumentNotValidException e){return ResponseEntity.badRequest().body(new Problem("INVALID_REQUEST",e.getBindingResult().getAllErrors().get(0).getDefaultMessage(),Instant.now()));}
  @ExceptionHandler(IdempotencyConflictException.class) ResponseEntity<Problem> conflict(IdempotencyConflictException e){return ResponseEntity.status(409).body(new Problem("IDEMPOTENCY_CONFLICT",e.getMessage(),Instant.now()));}
  public record Problem(String code,String message,Instant timestamp){}
}
