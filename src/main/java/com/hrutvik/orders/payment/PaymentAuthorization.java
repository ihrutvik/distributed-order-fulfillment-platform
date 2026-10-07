package com.hrutvik.orders.payment;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="payment_authorizations",uniqueConstraints=@UniqueConstraint(name="uk_payment_authorization_order",columnNames="order_id"))
public class PaymentAuthorization {
  @Id private UUID id;
  @Column(name="order_id",nullable=false) private UUID orderId;
  @Column(nullable=false,precision=19,scale=2) private BigDecimal amount;
  @Column(nullable=false,length=3) private String currency;
  @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Status status;
  @Column(name="authorized_at",nullable=false) private Instant authorizedAt;
  @Column(name="voided_at") private Instant voidedAt;
  @Version private long version;
  protected PaymentAuthorization(){}
  public PaymentAuthorization(UUID orderId,BigDecimal amount,String currency){this.id=UUID.randomUUID();this.orderId=orderId;this.amount=amount;this.currency=currency;this.status=Status.AUTHORIZED;this.authorizedAt=Instant.now();}
  public void voidAuthorization(){if(status==Status.VOIDED)return;status=Status.VOIDED;voidedAt=Instant.now();}
  public UUID getId(){return id;} public UUID getOrderId(){return orderId;} public BigDecimal getAmount(){return amount;} public String getCurrency(){return currency;} public Status getStatus(){return status;}
  public enum Status {AUTHORIZED,VOIDED}
}
