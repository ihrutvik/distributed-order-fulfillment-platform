package com.hrutvik.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrutvik.orders.payment.*;
import com.hrutvik.orders.persistence.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentParticipantTest {
  @Test void authorizesPaymentAndEmitsDurableReply(){
    Fixture f=new Fixture();when(f.authorizations.save(any())).thenAnswer(i->i.getArgument(0));UUID orderId=UUID.randomUUID();
    f.participant.handle(new PaymentCommand(UUID.randomUUID(),orderId,"AUTHORIZE_PAYMENT",new BigDecimal("79.99"),"EUR",null));
    OutboxEvent event=f.captureEvent();assertThat(event.getEventType()).isEqualTo("PAYMENT_AUTHORIZED");assertThat(event.getDestination()).isEqualTo("SAGA_REPLIES");verify(f.receipts).save(any());
  }

  @Test void declinesPaymentAboveConfiguredLimit(){
    Fixture f=new Fixture();f.participant.handle(new PaymentCommand(UUID.randomUUID(),UUID.randomUUID(),"AUTHORIZE_PAYMENT",new BigDecimal("1000.01"),"EUR",null));
    assertThat(f.captureEvent().getEventType()).isEqualTo("PAYMENT_DECLINED");verify(f.authorizations,never()).save(any());
  }

  @Test void voidIsIdempotentAtBusinessBoundary(){
    Fixture f=new Fixture();UUID orderId=UUID.randomUUID();PaymentAuthorization authorization=new PaymentAuthorization(orderId,new BigDecimal("20.00"),"EUR");when(f.authorizations.lockByOrderId(orderId)).thenReturn(Optional.of(authorization));
    f.participant.handle(new PaymentCommand(UUID.randomUUID(),orderId,"VOID_PAYMENT",null,null,authorization.getId().toString()));
    assertThat(authorization.getStatus()).isEqualTo(PaymentAuthorization.Status.VOIDED);assertThat(f.captureEvent().getEventType()).isEqualTo("PAYMENT_VOIDED");
  }

  @Test void duplicateCommandHasNoSideEffects(){
    Fixture f=new Fixture();UUID messageId=UUID.randomUUID();when(f.receipts.existsById(messageId)).thenReturn(true);
    f.participant.handle(new PaymentCommand(messageId,UUID.randomUUID(),"AUTHORIZE_PAYMENT",new BigDecimal("20.00"),"EUR",null));
    verifyNoInteractions(f.authorizations,f.outbox);
  }

  static class Fixture {
    final PaymentAuthorizationRepository authorizations=mock(PaymentAuthorizationRepository.class);final PaymentCommandReceiptRepository receipts=mock(PaymentCommandReceiptRepository.class);final OutboxRepository outbox=mock(OutboxRepository.class);
    final PaymentParticipant participant=new PaymentParticipant(authorizations,receipts,outbox,new ObjectMapper(),new BigDecimal("1000.00"));
    OutboxEvent captureEvent(){ArgumentCaptor<OutboxEvent> c=ArgumentCaptor.forClass(OutboxEvent.class);verify(outbox).save(c.capture());return c.getValue();}
  }
}
