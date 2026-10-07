package com.hrutvik.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrutvik.orders.application.*;
import com.hrutvik.orders.domain.*;
import com.hrutvik.orders.persistence.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SagaCoordinatorTest {
  @Test void advancesInventoryReplyAndEmitsPaymentCommand(){
    Fixture f=new Fixture(); SagaReply reply=new SagaReply(UUID.randomUUID(),f.order.getId(),"INVENTORY_RESERVED","inv-42",null);
    f.coordinator.handle(reply);
    assertThat(f.order.getStatus()).isEqualTo(OrderStatus.INVENTORY_RESERVED);
    OutboxEvent event=f.captureEvent();assertThat(event.getEventType()).isEqualTo("AUTHORIZE_PAYMENT");assertThat(event.getDestination()).isEqualTo("PAYMENT_COMMANDS");verify(f.inbox).save(any(InboxMessage.class));
  }
  @Test void shipmentFailureStartsReverseOrderCompensation(){
    Fixture f=new Fixture();f.order.inventoryReserved("inv-42");f.order.paymentAuthorized("pay-42");
    f.coordinator.handle(new SagaReply(UUID.randomUUID(),f.order.getId(),"SHIPMENT_REJECTED",null,"NO_CAPACITY"));
    assertThat(f.order.getStatus()).isEqualTo(OrderStatus.COMPENSATING);assertThat(f.order.getFailureCode()).isEqualTo("NO_CAPACITY");
    OutboxEvent event=f.captureEvent();assertThat(event.getEventType()).isEqualTo("VOID_PAYMENT");assertThat(event.getDestination()).isEqualTo("PAYMENT_COMMANDS");
  }
  @Test void paymentAuthorizationRoutesShipmentCommand(){
    Fixture f=new Fixture();f.order.inventoryReserved("inv-42");f.coordinator.handle(new SagaReply(UUID.randomUUID(),f.order.getId(),"PAYMENT_AUTHORIZED","pay-42",null));
    assertThat(f.order.getStatus()).isEqualTo(OrderStatus.PAYMENT_AUTHORIZED);OutboxEvent event=f.captureEvent();assertThat(event.getEventType()).isEqualTo("RESERVE_SHIPMENT");assertThat(event.getDestination()).isEqualTo("SHIPMENT_COMMANDS");
  }
  @Test void shipmentReservationConfirmsOrder(){
    Fixture f=new Fixture();f.order.inventoryReserved("inv-42");f.order.paymentAuthorized("pay-42");f.coordinator.handle(new SagaReply(UUID.randomUUID(),f.order.getId(),"SHIPMENT_RESERVED","ship-42",null));
    assertThat(f.order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);assertThat(f.captureEvent().getEventType()).isEqualTo("ORDER_CONFIRMED");
  }
  @Test void duplicateReplyHasNoSideEffects(){
    Fixture f=new Fixture();UUID messageId=UUID.randomUUID();when(f.inbox.existsById(messageId)).thenReturn(true);
    f.coordinator.handle(new SagaReply(messageId,f.order.getId(),"INVENTORY_RESERVED","inv-42",null));
    assertThat(f.order.getStatus()).isEqualTo(OrderStatus.PENDING);verifyNoInteractions(f.outbox);
  }
  @Test void rejectsOutOfOrderReplyAndDoesNotAcknowledgeIt(){
    Fixture f=new Fixture();assertThatThrownBy(()->f.coordinator.handle(new SagaReply(UUID.randomUUID(),f.order.getId(),"PAYMENT_AUTHORIZED","pay-42",null))).isInstanceOf(IllegalStateException.class);
    verify(f.inbox,never()).save(any());
  }
  static class Fixture {
    final FulfillmentOrderRepository orders=mock(FulfillmentOrderRepository.class);final InboxRepository inbox=mock(InboxRepository.class);final OutboxRepository outbox=mock(OutboxRepository.class);final SagaDeadlineRepository deadlines=mock(SagaDeadlineRepository.class);
    final FulfillmentOrder order=new FulfillmentOrder(UUID.randomUUID(),"merchant-1","checkout-1",new BigDecimal("49.99"),"EUR");
    final SagaCoordinator coordinator=new SagaCoordinator(orders,inbox,outbox,deadlines,new ObjectMapper(),mock(com.hrutvik.orders.observability.OrderMetrics.class));
    Fixture(){when(orders.findById(order.getId())).thenReturn(Optional.of(order));when(deadlines.findByOrderId(order.getId())).thenReturn(Optional.empty());}
    OutboxEvent captureEvent(){ArgumentCaptor<OutboxEvent> c=ArgumentCaptor.forClass(OutboxEvent.class);verify(outbox).save(c.capture());return c.getValue();}
  }
}
