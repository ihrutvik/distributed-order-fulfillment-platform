package com.hrutvik.orders.shipment;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="shipment_command_receipts")
public class ShipmentCommandReceipt {
  @Id @Column(name="message_id") private UUID messageId;
  @Column(name="handled_at",nullable=false) private Instant handledAt;
  protected ShipmentCommandReceipt(){}
  public ShipmentCommandReceipt(UUID messageId){this.messageId=messageId;this.handledAt=Instant.now();}
}
