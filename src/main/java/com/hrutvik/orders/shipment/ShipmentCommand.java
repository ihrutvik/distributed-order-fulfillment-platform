package com.hrutvik.orders.shipment;

import java.util.UUID;

public record ShipmentCommand(UUID messageId,UUID orderId,String type,String paymentAuthorizationId,String zone) {}
