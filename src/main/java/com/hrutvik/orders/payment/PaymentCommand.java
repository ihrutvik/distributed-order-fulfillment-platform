package com.hrutvik.orders.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentCommand(UUID messageId,UUID orderId,String type,BigDecimal amount,String currency,String authorizationId) {}
