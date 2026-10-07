package com.hrutvik.orders.inventory;

import java.util.UUID;

public record InventoryCommand(UUID messageId,UUID orderId,String type,String sku,Integer quantity,String reservationId) {}
