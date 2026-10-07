package com.hrutvik.orders.inventory;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="inventory_command_receipts")
public class InventoryCommandReceipt {
  @Id @Column(name="message_id") private UUID messageId;
  @Column(name="handled_at",nullable=false) private Instant handledAt;
  protected InventoryCommandReceipt(){}
  public InventoryCommandReceipt(UUID messageId){this.messageId=messageId;this.handledAt=Instant.now();}
}
