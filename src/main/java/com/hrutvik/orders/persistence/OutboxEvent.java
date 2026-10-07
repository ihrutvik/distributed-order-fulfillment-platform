package com.hrutvik.orders.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="outbox_events")
public class OutboxEvent {
  @Id private UUID id;
  @Column(name="aggregate_id",nullable=false) private UUID aggregateId;
  @Column(name="event_type",nullable=false,length=100) private String eventType;
  @Column(nullable=false,columnDefinition="text") private String payload;
  @Column(nullable=false,length=40) private String destination;
  @Column(name="created_at",nullable=false) private Instant createdAt;
  @Column(name="published_at") private Instant publishedAt;
  protected OutboxEvent(){}
  public OutboxEvent(UUID aggregateId,String eventType,String payload){this(aggregateId,eventType,payload,"LIFECYCLE");}
  public OutboxEvent(UUID aggregateId,String eventType,String payload,String destination){id=UUID.randomUUID();this.aggregateId=aggregateId;this.eventType=eventType;this.payload=payload;this.destination=destination;createdAt=Instant.now();}
  public void published(){publishedAt=Instant.now();}
  public UUID getId(){return id;} public UUID getAggregateId(){return aggregateId;} public String getEventType(){return eventType;} public String getPayload(){return payload;} public String getDestination(){return destination;}
}
