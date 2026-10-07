package com.hrutvik.orders.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrutvik.orders.persistence.*;
import com.hrutvik.orders.observability.OrderMetrics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Component
public class SagaCoordinator {
  private final FulfillmentOrderRepository orders; private final InboxRepository inbox; private final OutboxRepository outbox; private final SagaDeadlineRepository deadlines; private final ObjectMapper json; private final OrderMetrics metrics;
  public SagaCoordinator(FulfillmentOrderRepository orders,InboxRepository inbox,OutboxRepository outbox,SagaDeadlineRepository deadlines,ObjectMapper json,OrderMetrics metrics){this.orders=orders;this.inbox=inbox;this.outbox=outbox;this.deadlines=deadlines;this.json=json;this.metrics=metrics;}

  @KafkaListener(topics="${orders.saga.replies-topic}",groupId="${orders.saga.consumer-group}")
  @Transactional
  public void receive(String payload){try{handle(json.readValue(payload,SagaReply.class));}catch(JsonProcessingException e){throw new IllegalArgumentException("Invalid saga reply",e);}}

  @Transactional
  public void handle(SagaReply reply){
    if(inbox.existsById(reply.messageId())){metrics.sagaReply(reply.type(),"duplicate");return;}
    var order=orders.findById(reply.orderId()).orElseThrow(()->new IllegalArgumentException("Unknown order "+reply.orderId()));
    switch(reply.type()){
      case "INVENTORY_RESERVED" -> {order.inventoryReserved(required(reply.reference(),reply.type()));command(order.getId(),"AUTHORIZE_PAYMENT",Map.of("amount",order.getAmount(),"currency",order.getCurrency()));}
      case "INVENTORY_REJECTED" -> {order.beginCompensation(code(reply));order.cancel();command(order.getId(),"ORDER_CANCELLED",Map.of("failureCode",code(reply)));}
      case "PAYMENT_AUTHORIZED" -> {order.paymentAuthorized(required(reply.reference(),reply.type()));command(order.getId(),"RESERVE_SHIPMENT",Map.of("paymentAuthorizationId",reply.reference()));}
      case "PAYMENT_DECLINED" -> {order.beginCompensation(code(reply));command(order.getId(),"RELEASE_INVENTORY",Map.of("inventoryReservationId",order.getInventoryReservationId()));}
      case "SHIPMENT_RESERVED" -> {order.shipmentReserved(required(reply.reference(),reply.type()));order.confirm();command(order.getId(),"ORDER_CONFIRMED",Map.of("shipmentReservationId",reply.reference()));}
      case "SHIPMENT_REJECTED" -> {order.beginCompensation(code(reply));command(order.getId(),"VOID_PAYMENT",Map.of("paymentAuthorizationId",order.getPaymentAuthorizationId()));}
      case "PAYMENT_VOIDED" -> command(order.getId(),"RELEASE_INVENTORY",Map.of("inventoryReservationId",order.getInventoryReservationId()));
      case "INVENTORY_RELEASED" -> {order.cancel();command(order.getId(),"ORDER_CANCELLED",Map.of("failureCode",order.getFailureCode()));}
      default -> throw new IllegalArgumentException("Unsupported saga reply "+reply.type());
    }
    inbox.save(new InboxMessage(reply.messageId(),reply.orderId(),reply.type()));
    metrics.sagaReply(reply.type(),"applied");
  }

  private void command(java.util.UUID orderId,String type,Map<String,?> details){
    try{
      OutboxEvent event;
      if(type.equals("RELEASE_INVENTORY")){
        Map<String,Object> payload=new LinkedHashMap<>();payload.put("messageId",java.util.UUID.randomUUID());payload.put("orderId",orderId);payload.put("type",type);payload.put("reservationId",details.get("inventoryReservationId"));
        event=new OutboxEvent(orderId,type,json.writeValueAsString(payload),"INVENTORY_COMMANDS");
      }else if(type.equals("AUTHORIZE_PAYMENT") || type.equals("VOID_PAYMENT")){
        Map<String,Object> payload=new LinkedHashMap<>();payload.put("messageId",java.util.UUID.randomUUID());payload.put("orderId",orderId);payload.put("type",type);payload.put("amount",details.get("amount"));payload.put("currency",details.get("currency"));payload.put("authorizationId",details.get("paymentAuthorizationId"));
        event=new OutboxEvent(orderId,type,json.writeValueAsString(payload),"PAYMENT_COMMANDS");
      }else if(type.equals("RESERVE_SHIPMENT")){
        Map<String,Object> payload=new LinkedHashMap<>();payload.put("messageId",java.util.UUID.randomUUID());payload.put("orderId",orderId);payload.put("type",type);payload.put("paymentAuthorizationId",details.get("paymentAuthorizationId"));payload.put("zone","DEFAULT");
        event=new OutboxEvent(orderId,type,json.writeValueAsString(payload),"SHIPMENT_COMMANDS");
      }else event=new OutboxEvent(orderId,type,json.writeValueAsString(Map.of("orderId",orderId,"type",type,"details",details)));
      outbox.save(event);track(orderId,event);
    }
    catch(JsonProcessingException e){throw new IllegalStateException("Could not serialize saga command",e);}
  }
  private void track(java.util.UUID orderId,OutboxEvent event){
    var existing=deadlines.findByOrderId(orderId);
    if(event.getDestination().equals("LIFECYCLE")){existing.ifPresent(SagaDeadline::complete);return;}
    SagaDeadline deadline=existing.orElseGet(()->new SagaDeadline(orderId,event.getEventType(),event.getPayload(),event.getDestination(),Instant.now().plusSeconds(30)));
    if(existing.isPresent())deadline.schedule(event.getEventType(),event.getPayload(),event.getDestination(),Instant.now().plusSeconds(30));
    deadlines.save(deadline);
  }
  private String code(SagaReply reply){return reply.failureCode()==null?"PARTICIPANT_REJECTED":reply.failureCode();}
  private String required(String value,String type){if(value==null || value.isBlank()) throw new IllegalArgumentException(type+" requires a reference");return value;}
}
