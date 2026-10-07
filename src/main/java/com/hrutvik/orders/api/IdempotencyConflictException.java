package com.hrutvik.orders.api;

public class IdempotencyConflictException extends RuntimeException {
  public IdempotencyConflictException(String clientOrderId){super("Client order ID '"+clientOrderId+"' was already used with a different request");}
}
