package com.hrutvik.orders.inventory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrutvik.orders.application.SagaReply;
import com.hrutvik.orders.persistence.OutboxEvent;
import com.hrutvik.orders.persistence.OutboxRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class InventoryParticipant {
  private final InventoryStockRepository stock; private final InventoryReservationRepository reservations; private final InventoryCommandReceiptRepository receipts; private final OutboxRepository outbox; private final ObjectMapper json;
  public InventoryParticipant(InventoryStockRepository stock,InventoryReservationRepository reservations,InventoryCommandReceiptRepository receipts,OutboxRepository outbox,ObjectMapper json){this.stock=stock;this.reservations=reservations;this.receipts=receipts;this.outbox=outbox;this.json=json;}

  @KafkaListener(topics="${orders.saga.inventory-commands-topic}",groupId="inventory-participant")
  @Transactional
  public void receive(String payload){try{handle(json.readValue(payload,InventoryCommand.class));}catch(JsonProcessingException e){throw new IllegalArgumentException("Invalid inventory command",e);}}

  @Transactional
  public void handle(InventoryCommand command){
    if(receipts.existsById(command.messageId()))return;
    switch(command.type()){
      case "RESERVE_INVENTORY" -> reserve(command);
      case "RELEASE_INVENTORY" -> release(command);
      default -> throw new IllegalArgumentException("Unsupported inventory command "+command.type());
    }
    receipts.save(new InventoryCommandReceipt(command.messageId()));
  }

  private void reserve(InventoryCommand command){
    if(command.sku()==null || command.quantity()==null || command.quantity()<1)throw new IllegalArgumentException("Reservation requires SKU and positive quantity");
    var existing=reservations.findByOrderId(command.orderId());
    if(existing.isPresent()){reply(command.orderId(),"INVENTORY_RESERVED",existing.get().getId().toString(),null);return;}
    var item=stock.lockBySku(command.sku()).orElse(null);
    if(item==null){reply(command.orderId(),"INVENTORY_REJECTED",null,"UNKNOWN_SKU");return;}
    if(!item.reserve(command.quantity())){reply(command.orderId(),"INVENTORY_REJECTED",null,"INSUFFICIENT_STOCK");return;}
    var reservation=reservations.save(new InventoryReservation(command.orderId(),command.sku(),command.quantity()));
    reply(command.orderId(),"INVENTORY_RESERVED",reservation.getId().toString(),null);
  }

  private void release(InventoryCommand command){
    var reservation=reservations.findByOrderId(command.orderId()).orElseThrow(()->new IllegalArgumentException("No inventory reservation for order "+command.orderId()));
    if(reservation.getStatus()==InventoryReservation.Status.RESERVED){
      var item=stock.lockBySku(reservation.getSku()).orElseThrow(()->new IllegalStateException("Stock row missing for "+reservation.getSku()));
      item.release(reservation.getQuantity());reservation.release();
    }
    reply(command.orderId(),"INVENTORY_RELEASED",reservation.getId().toString(),null);
  }

  private void reply(UUID orderId,String type,String reference,String failureCode){
    try{SagaReply reply=new SagaReply(UUID.randomUUID(),orderId,type,reference,failureCode);outbox.save(new OutboxEvent(orderId,type,json.writeValueAsString(reply),"SAGA_REPLIES"));}
    catch(JsonProcessingException e){throw new IllegalStateException("Could not serialize inventory reply",e);}
  }
}
