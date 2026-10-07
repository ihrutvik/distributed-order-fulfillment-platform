package com.hrutvik.orders;

import com.hrutvik.orders.application.OrderService;
import com.hrutvik.orders.application.OrderRequestFingerprint;
import com.hrutvik.orders.api.IdempotencyConflictException;
import com.hrutvik.orders.domain.FulfillmentOrder;
import com.hrutvik.orders.persistence.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {
  @Test void createsOrderAndEventAtomically(){
    var orders=mock(FulfillmentOrderRepository.class);var outbox=mock(OutboxRepository.class);var deadlines=mock(SagaDeadlineRepository.class);when(orders.findByMerchantIdAndClientOrderId("m-1","o-1")).thenReturn(Optional.empty());when(orders.save(any())).thenAnswer(i->i.getArgument(0));
    FulfillmentOrder order=new OrderService(orders,outbox,deadlines,mock(com.hrutvik.orders.observability.OrderMetrics.class)).create("m-1","o-1",new BigDecimal("20.00"),"EUR");
    assertThat(order.getStatus().name()).isEqualTo("PENDING");ArgumentCaptor<OutboxEvent> event=ArgumentCaptor.forClass(OutboxEvent.class);verify(outbox).save(event.capture());assertThat(event.getValue().getEventType()).isEqualTo("RESERVE_INVENTORY");assertThat(event.getValue().getDestination()).isEqualTo("INVENTORY_COMMANDS");verify(deadlines).save(any(SagaDeadline.class));
  }
  @Test void idempotentReplayReturnsExistingOrder(){
    var orders=mock(FulfillmentOrderRepository.class);var outbox=mock(OutboxRepository.class);var existing=new FulfillmentOrder(UUID.randomUUID(),"m-1","o-1",new BigDecimal("20.00"),"EUR",OrderRequestFingerprint.calculate("m-1","o-1",new BigDecimal("20.00"),"EUR"));when(orders.findByMerchantIdAndClientOrderId("m-1","o-1")).thenReturn(Optional.of(existing));
    assertThat(new OrderService(orders,outbox,mock(SagaDeadlineRepository.class),mock(com.hrutvik.orders.observability.OrderMetrics.class)).create("m-1","o-1",new BigDecimal("20.00"),"EUR")).isSameAs(existing);verifyNoInteractions(outbox);
  }
  @Test void rejectsIdempotencyKeyReuseWithDifferentPayload(){
    var orders=mock(FulfillmentOrderRepository.class);var outbox=mock(OutboxRepository.class);var existing=new FulfillmentOrder(UUID.randomUUID(),"m-1","o-1",new BigDecimal("20.00"),"EUR",OrderRequestFingerprint.calculate("m-1","o-1",new BigDecimal("20.00"),"EUR"));when(orders.findByMerchantIdAndClientOrderId("m-1","o-1")).thenReturn(Optional.of(existing));
    org.assertj.core.api.Assertions.assertThatThrownBy(()->new OrderService(orders,outbox,mock(SagaDeadlineRepository.class),mock(com.hrutvik.orders.observability.OrderMetrics.class)).create("m-1","o-1",new BigDecimal("21.00"),"EUR")).isInstanceOf(IdempotencyConflictException.class);verifyNoInteractions(outbox);
  }
  @Test void canonicalFingerprintTreatsEquivalentAmountsAndCurrencyAsEqual(){
    assertThat(OrderRequestFingerprint.calculate("m-1","o-1",new BigDecimal("20.00"),"eur")).isEqualTo(OrderRequestFingerprint.calculate("m-1","o-1",new BigDecimal("20"),"EUR"));
  }
}
