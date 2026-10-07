package com.hrutvik.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrutvik.orders.application.SagaTimeoutWorker;
import com.hrutvik.orders.persistence.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SagaTimeoutWorkerTest {
  @Test void republishesOriginalCommandAndAppliesBackoff(){
    var deadlines=mock(SagaDeadlineRepository.class);var outbox=mock(OutboxRepository.class);var deadline=deadline("AUTHORIZE_PAYMENT","PAYMENT_COMMANDS");when(deadlines.lockDue(any(),eq(50))).thenReturn(List.of(deadline));
    new SagaTimeoutWorker(deadlines,outbox,new ObjectMapper(),mock(com.hrutvik.orders.observability.OrderMetrics.class),3,Duration.ofSeconds(10),50).retryDue();
    ArgumentCaptor<OutboxEvent> event=ArgumentCaptor.forClass(OutboxEvent.class);verify(outbox).save(event.capture());assertThat(event.getValue().getEventType()).isEqualTo("AUTHORIZE_PAYMENT");assertThat(event.getValue().getDestination()).isEqualTo("PAYMENT_COMMANDS");assertThat(deadline.getAttempts()).isEqualTo(1);
  }

  @Test void exhaustedForwardStageEmitsSyntheticRejectionForCompensation(){
    var deadlines=mock(SagaDeadlineRepository.class);var outbox=mock(OutboxRepository.class);var deadline=deadline("RESERVE_SHIPMENT","SHIPMENT_COMMANDS");deadline.retried(Instant.now());deadline.retried(Instant.now());deadline.retried(Instant.now());when(deadlines.lockDue(any(),eq(50))).thenReturn(List.of(deadline));
    new SagaTimeoutWorker(deadlines,outbox,new ObjectMapper(),mock(com.hrutvik.orders.observability.OrderMetrics.class),3,Duration.ofSeconds(10),50).retryDue();
    ArgumentCaptor<OutboxEvent> event=ArgumentCaptor.forClass(OutboxEvent.class);verify(outbox).save(event.capture());assertThat(event.getValue().getEventType()).isEqualTo("SHIPMENT_REJECTED");assertThat(event.getValue().getDestination()).isEqualTo("SAGA_REPLIES");assertThat(deadline.isCompleted()).isTrue();
  }

  @Test void exhaustedCompensationRaisesVisibleStalledEvent(){
    var deadlines=mock(SagaDeadlineRepository.class);var outbox=mock(OutboxRepository.class);var deadline=deadline("VOID_PAYMENT","PAYMENT_COMMANDS");deadline.retried(Instant.now());deadline.retried(Instant.now());deadline.retried(Instant.now());when(deadlines.lockDue(any(),eq(50))).thenReturn(List.of(deadline));
    new SagaTimeoutWorker(deadlines,outbox,new ObjectMapper(),mock(com.hrutvik.orders.observability.OrderMetrics.class),3,Duration.ofSeconds(10),50).retryDue();
    ArgumentCaptor<OutboxEvent> event=ArgumentCaptor.forClass(OutboxEvent.class);verify(outbox).save(event.capture());assertThat(event.getValue().getEventType()).isEqualTo("COMPENSATION_STALLED");assertThat(event.getValue().getDestination()).isEqualTo("LIFECYCLE");
  }

  private SagaDeadline deadline(String stage,String destination){UUID orderId=UUID.randomUUID();return new SagaDeadline(orderId,stage,"{\"messageId\":\""+UUID.randomUUID()+"\",\"orderId\":\""+orderId+"\",\"type\":\""+stage+"\"}",destination,Instant.now().minusSeconds(1));}
}
