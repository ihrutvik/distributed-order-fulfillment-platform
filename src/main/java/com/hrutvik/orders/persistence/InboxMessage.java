package com.hrutvik.orders.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="inbox_messages")
public class InboxMessage {
  @Id @Column(name="message_id") private UUID messageId;
  @Column(name="order_id",nullable=false) private UUID orderId;
  @Column(name="event_type",nullable=false,length=100) private String eventType;
  @Column(name="handled_at",nullable=false) private Instant handledAt;
  protected InboxMessage(){}
  public InboxMessage(UUID messageId,UUID orderId,String eventType){this.messageId=messageId;this.orderId=orderId;this.eventType=eventType;this.handledAt=Instant.now();}
}
