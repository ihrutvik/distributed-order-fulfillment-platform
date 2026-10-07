package com.hrutvik.orders.payment;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="payment_command_receipts")
public class PaymentCommandReceipt {
  @Id @Column(name="message_id") private UUID messageId;
  @Column(name="handled_at",nullable=false) private Instant handledAt;
  protected PaymentCommandReceipt(){}
  public PaymentCommandReceipt(UUID messageId){this.messageId=messageId;this.handledAt=Instant.now();}
}
