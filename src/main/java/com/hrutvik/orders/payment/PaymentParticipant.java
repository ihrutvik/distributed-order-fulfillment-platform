package com.hrutvik.orders.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrutvik.orders.application.SagaReply;
import com.hrutvik.orders.persistence.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PaymentParticipant {
  private final PaymentAuthorizationRepository authorizations; private final PaymentCommandReceiptRepository receipts; private final OutboxRepository outbox; private final ObjectMapper json; private final BigDecimal authorizationLimit;
  public PaymentParticipant(PaymentAuthorizationRepository authorizations,PaymentCommandReceiptRepository receipts,OutboxRepository outbox,ObjectMapper json,@Value("${orders.payment.authorization-limit:10000.00}") BigDecimal authorizationLimit){this.authorizations=authorizations;this.receipts=receipts;this.outbox=outbox;this.json=json;this.authorizationLimit=authorizationLimit;}

  @KafkaListener(topics="${orders.saga.payment-commands-topic}",groupId="payment-participant")
  @Transactional
  public void receive(String payload){try{handle(json.readValue(payload,PaymentCommand.class));}catch(JsonProcessingException e){throw new IllegalArgumentException("Invalid payment command",e);}}

  @Transactional
  public void handle(PaymentCommand command){
    if(receipts.existsById(command.messageId()))return;
    switch(command.type()){
      case "AUTHORIZE_PAYMENT" -> authorize(command);
      case "VOID_PAYMENT" -> voidPayment(command);
      default -> throw new IllegalArgumentException("Unsupported payment command "+command.type());
    }
    receipts.save(new PaymentCommandReceipt(command.messageId()));
  }

  private void authorize(PaymentCommand command){
    if(command.amount()==null || command.amount().signum()<=0 || command.currency()==null)throw new IllegalArgumentException("Authorization requires amount and currency");
    var existing=authorizations.findByOrderId(command.orderId());
    if(existing.isPresent()){
      if(existing.get().getStatus()==PaymentAuthorization.Status.VOIDED)reply(command.orderId(),"PAYMENT_DECLINED",null,"AUTHORIZATION_ALREADY_VOIDED");
      else reply(command.orderId(),"PAYMENT_AUTHORIZED",existing.get().getId().toString(),null);
      return;
    }
    if(command.amount().compareTo(authorizationLimit)>0){reply(command.orderId(),"PAYMENT_DECLINED",null,"AUTHORIZATION_LIMIT_EXCEEDED");return;}
    var authorization=authorizations.save(new PaymentAuthorization(command.orderId(),command.amount(),command.currency().toUpperCase()));
    reply(command.orderId(),"PAYMENT_AUTHORIZED",authorization.getId().toString(),null);
  }

  private void voidPayment(PaymentCommand command){
    var authorization=authorizations.lockByOrderId(command.orderId()).orElseThrow(()->new IllegalArgumentException("No payment authorization for order "+command.orderId()));
    authorization.voidAuthorization();
    reply(command.orderId(),"PAYMENT_VOIDED",authorization.getId().toString(),null);
  }

  private void reply(UUID orderId,String type,String reference,String failureCode){
    try{SagaReply reply=new SagaReply(UUID.randomUUID(),orderId,type,reference,failureCode);outbox.save(new OutboxEvent(orderId,type,json.writeValueAsString(reply),"SAGA_REPLIES"));}
    catch(JsonProcessingException e){throw new IllegalStateException("Could not serialize payment reply",e);}
  }
}
