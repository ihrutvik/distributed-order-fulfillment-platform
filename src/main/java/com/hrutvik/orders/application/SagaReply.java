package com.hrutvik.orders.application;

import java.util.UUID;

public record SagaReply(UUID messageId,UUID orderId,String type,String reference,String failureCode) {}
