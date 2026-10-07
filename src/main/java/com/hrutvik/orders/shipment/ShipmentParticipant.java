package com.hrutvik.orders.shipment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrutvik.orders.application.SagaReply;
import com.hrutvik.orders.persistence.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class ShipmentParticipant {
  private final ShipmentCapacityRepository capacity; private final ShipmentReservationRepository reservations; private final ShipmentCommandReceiptRepository receipts; private final OutboxRepository outbox; private final ObjectMapper json; private final String defaultZone;
  public ShipmentParticipant(ShipmentCapacityRepository capacity,ShipmentReservationRepository reservations,ShipmentCommandReceiptRepository receipts,OutboxRepository outbox,ObjectMapper json,@Value("${orders.shipment.default-zone:DEFAULT}") String defaultZone){this.capacity=capacity;this.reservations=reservations;this.receipts=receipts;this.outbox=outbox;this.json=json;this.defaultZone=defaultZone;}

  @KafkaListener(topics="${orders.saga.shipment-commands-topic}",groupId="shipment-participant")
  @Transactional
  public void receive(String payload){try{handle(json.readValue(payload,ShipmentCommand.class));}catch(JsonProcessingException e){throw new IllegalArgumentException("Invalid shipment command",e);}}

  @Transactional
  public void handle(ShipmentCommand command){
    if(receipts.existsById(command.messageId()))return;
    if(!"RESERVE_SHIPMENT".equals(command.type()))throw new IllegalArgumentException("Unsupported shipment command "+command.type());
    reserve(command);
    receipts.save(new ShipmentCommandReceipt(command.messageId()));
  }

  private void reserve(ShipmentCommand command){
    if(command.paymentAuthorizationId()==null || command.paymentAuthorizationId().isBlank())throw new IllegalArgumentException("Shipment requires a payment authorization");
    var existing=reservations.findByOrderId(command.orderId());
    if(existing.isPresent()){reply(command.orderId(),"SHIPMENT_RESERVED",existing.get().getId().toString(),null);return;}
    String zone=command.zone()==null || command.zone().isBlank()?defaultZone:command.zone().toUpperCase();
    var slots=capacity.lockByZone(zone).orElse(null);
    if(slots==null){reply(command.orderId(),"SHIPMENT_REJECTED",null,"UNSUPPORTED_ZONE");return;}
    if(!slots.reserve()){reply(command.orderId(),"SHIPMENT_REJECTED",null,"NO_SHIPMENT_CAPACITY");return;}
    var reservation=reservations.save(new ShipmentReservation(command.orderId(),zone,command.paymentAuthorizationId()));
    reply(command.orderId(),"SHIPMENT_RESERVED",reservation.getId().toString(),null);
  }

  private void reply(UUID orderId,String type,String reference,String failureCode){
    try{SagaReply reply=new SagaReply(UUID.randomUUID(),orderId,type,reference,failureCode);outbox.save(new OutboxEvent(orderId,type,json.writeValueAsString(reply),"SAGA_REPLIES"));}
    catch(JsonProcessingException e){throw new IllegalStateException("Could not serialize shipment reply",e);}
  }
}
