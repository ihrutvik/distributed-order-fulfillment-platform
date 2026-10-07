package com.hrutvik.orders.persistence;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity @Table(name="saga_deadlines",uniqueConstraints=@UniqueConstraint(name="uk_saga_deadline_order",columnNames="order_id"))
public class SagaDeadline {
  @Id private UUID id;
  @Column(name="order_id",nullable=false) private UUID orderId;
  @Column(nullable=false,length=100) private String stage;
  @Column(nullable=false,columnDefinition="text") private String payload;
  @Column(nullable=false,length=40) private String destination;
  @Column(nullable=false) private int attempts;
  @Column(name="next_attempt_at",nullable=false) private Instant nextAttemptAt;
  @Column(nullable=false) private boolean completed;
  @Column(name="updated_at",nullable=false) private Instant updatedAt;
  @Version private long version;
  protected SagaDeadline(){}
  public SagaDeadline(UUID orderId,String stage,String payload,String destination,Instant nextAttemptAt){this.id=UUID.randomUUID();this.orderId=orderId;schedule(stage,payload,destination,nextAttemptAt);}
  public void schedule(String stage,String payload,String destination,Instant nextAttemptAt){this.stage=stage;this.payload=payload;this.destination=destination;this.attempts=0;this.nextAttemptAt=nextAttemptAt;this.completed=false;this.updatedAt=Instant.now();}
  public void retried(Instant nextAttemptAt){attempts++;this.nextAttemptAt=nextAttemptAt;updatedAt=Instant.now();}
  public void complete(){completed=true;updatedAt=Instant.now();}
  public UUID getOrderId(){return orderId;} public String getStage(){return stage;} public String getPayload(){return payload;} public String getDestination(){return destination;} public int getAttempts(){return attempts;} public Instant getNextAttemptAt(){return nextAttemptAt;} public boolean isCompleted(){return completed;}
}
