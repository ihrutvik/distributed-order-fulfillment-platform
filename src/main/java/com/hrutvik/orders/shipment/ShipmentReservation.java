package com.hrutvik.orders.shipment;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="shipment_reservations",uniqueConstraints=@UniqueConstraint(name="uk_shipment_reservation_order",columnNames="order_id"))
public class ShipmentReservation {
  @Id private UUID id;
  @Column(name="order_id",nullable=false) private UUID orderId;
  @Column(nullable=false,length=50) private String zone;
  @Column(name="payment_authorization_id",nullable=false,length=255) private String paymentAuthorizationId;
  @Column(name="reserved_at",nullable=false) private Instant reservedAt;
  protected ShipmentReservation(){}
  public ShipmentReservation(UUID orderId,String zone,String paymentAuthorizationId){this.id=UUID.randomUUID();this.orderId=orderId;this.zone=zone;this.paymentAuthorizationId=paymentAuthorizationId;this.reservedAt=Instant.now();}
  public UUID getId(){return id;} public UUID getOrderId(){return orderId;} public String getZone(){return zone;} public String getPaymentAuthorizationId(){return paymentAuthorizationId;}
}
