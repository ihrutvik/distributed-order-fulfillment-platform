package com.hrutvik.orders.inventory;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="inventory_reservations",uniqueConstraints=@UniqueConstraint(name="uk_inventory_reservation_order",columnNames="order_id"))
public class InventoryReservation {
  @Id private UUID id;
  @Column(name="order_id",nullable=false) private UUID orderId;
  @Column(nullable=false,length=100) private String sku;
  @Column(nullable=false) private int quantity;
  @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Status status;
  @Column(name="created_at",nullable=false) private Instant createdAt;
  @Column(name="released_at") private Instant releasedAt;
  protected InventoryReservation(){}
  public InventoryReservation(UUID orderId,String sku,int quantity){this.id=UUID.randomUUID();this.orderId=orderId;this.sku=sku;this.quantity=quantity;this.status=Status.RESERVED;this.createdAt=Instant.now();}
  public void release(){if(status==Status.RELEASED)return;status=Status.RELEASED;releasedAt=Instant.now();}
  public UUID getId(){return id;} public UUID getOrderId(){return orderId;} public String getSku(){return sku;} public int getQuantity(){return quantity;} public Status getStatus(){return status;}
  public enum Status {RESERVED,RELEASED}
}
