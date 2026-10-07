package com.hrutvik.orders.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="fulfillment_orders",uniqueConstraints=@UniqueConstraint(name="uk_order_merchant_request",columnNames={"merchant_id","client_order_id"}))
public class FulfillmentOrder {
  @Id private UUID id;
  @Column(name="merchant_id",nullable=false,length=100) private String merchantId;
  @Column(name="client_order_id",nullable=false,length=100) private String clientOrderId;
  @Column(nullable=false,precision=19,scale=2) private BigDecimal amount;
  @Column(nullable=false,length=3) private String currency;
  @Column(nullable=false,length=100) private String sku;
  @Column(nullable=false) private int quantity;
  @Column(name="request_fingerprint",length=64) private String requestFingerprint;
  @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private OrderStatus status;
  @Column(name="inventory_reservation_id") private String inventoryReservationId;
  @Column(name="payment_authorization_id") private String paymentAuthorizationId;
  @Column(name="shipment_reservation_id") private String shipmentReservationId;
  @Column(name="failure_code") private String failureCode;
  @Version private long version;
  @Column(name="created_at",nullable=false) private Instant createdAt;
  @Column(name="updated_at",nullable=false) private Instant updatedAt;
  protected FulfillmentOrder(){}
  public FulfillmentOrder(UUID id,String merchantId,String clientOrderId,BigDecimal amount,String currency){
    this(id,merchantId,clientOrderId,amount,currency,"DEMO-SKU",1,null);
  }
  public FulfillmentOrder(UUID id,String merchantId,String clientOrderId,BigDecimal amount,String currency,String requestFingerprint){
    this(id,merchantId,clientOrderId,amount,currency,"DEMO-SKU",1,requestFingerprint);
  }
  public FulfillmentOrder(UUID id,String merchantId,String clientOrderId,BigDecimal amount,String currency,String sku,int quantity,String requestFingerprint){
    this.id=id;this.merchantId=merchantId;this.clientOrderId=clientOrderId;this.amount=amount;this.currency=currency.toUpperCase();this.status=OrderStatus.PENDING;this.createdAt=Instant.now();this.updatedAt=createdAt;
    this.sku=sku;this.quantity=quantity;this.requestFingerprint=requestFingerprint;
  }
  public void inventoryReserved(String reference){require(OrderStatus.PENDING);inventoryReservationId=reference;move(OrderStatus.INVENTORY_RESERVED);}
  public void paymentAuthorized(String reference){require(OrderStatus.INVENTORY_RESERVED);paymentAuthorizationId=reference;move(OrderStatus.PAYMENT_AUTHORIZED);}
  public void shipmentReserved(String reference){require(OrderStatus.PAYMENT_AUTHORIZED);shipmentReservationId=reference;move(OrderStatus.SHIPMENT_RESERVED);}
  public void confirm(){require(OrderStatus.SHIPMENT_RESERVED);move(OrderStatus.CONFIRMED);}
  public void beginCompensation(String code){if(status==OrderStatus.CONFIRMED || status==OrderStatus.CANCELLED) throw new IllegalStateException("Terminal order cannot be compensated");failureCode=code;move(OrderStatus.COMPENSATING);}
  public void cancel(){require(OrderStatus.COMPENSATING);move(OrderStatus.CANCELLED);}
  private void require(OrderStatus expected){if(status!=expected) throw new IllegalStateException("Expected "+expected+" but was "+status);}
  private void move(OrderStatus next){status=next;updatedAt=Instant.now();}
  public UUID getId(){return id;} public String getMerchantId(){return merchantId;} public String getClientOrderId(){return clientOrderId;}
  public BigDecimal getAmount(){return amount;} public String getCurrency(){return currency;} public OrderStatus getStatus(){return status;}
  public String getSku(){return sku;} public int getQuantity(){return quantity;}
  public String getRequestFingerprint(){return requestFingerprint;}
  public String getInventoryReservationId(){return inventoryReservationId;} public String getPaymentAuthorizationId(){return paymentAuthorizationId;}
  public String getShipmentReservationId(){return shipmentReservationId;} public String getFailureCode(){return failureCode;} public long getVersion(){return version;}
  public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
