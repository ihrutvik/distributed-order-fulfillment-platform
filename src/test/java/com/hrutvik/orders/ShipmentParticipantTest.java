package com.hrutvik.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrutvik.orders.persistence.*;
import com.hrutvik.orders.shipment.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ShipmentParticipantTest {
  @Test void reservesCapacityAndEmitsSagaReply(){
    Fixture f=new Fixture();ShipmentCapacity slots=new ShipmentCapacity("DEFAULT",2);when(f.capacity.lockByZone("DEFAULT")).thenReturn(Optional.of(slots));when(f.reservations.save(any())).thenAnswer(i->i.getArgument(0));
    f.participant.handle(new ShipmentCommand(UUID.randomUUID(),UUID.randomUUID(),"RESERVE_SHIPMENT","pay-42","DEFAULT"));
    assertThat(slots.getAvailableSlots()).isEqualTo(1);OutboxEvent event=f.captureEvent();assertThat(event.getEventType()).isEqualTo("SHIPMENT_RESERVED");assertThat(event.getDestination()).isEqualTo("SAGA_REPLIES");verify(f.receipts).save(any());
  }

  @Test void rejectsWhenCapacityIsExhausted(){
    Fixture f=new Fixture();ShipmentCapacity slots=new ShipmentCapacity("DEFAULT",0);when(f.capacity.lockByZone("DEFAULT")).thenReturn(Optional.of(slots));
    f.participant.handle(new ShipmentCommand(UUID.randomUUID(),UUID.randomUUID(),"RESERVE_SHIPMENT","pay-42","DEFAULT"));
    assertThat(f.captureEvent().getEventType()).isEqualTo("SHIPMENT_REJECTED");verify(f.reservations,never()).save(any());
  }

  @Test void businessReplayReturnsExistingReservationWithoutConsumingCapacity(){
    Fixture f=new Fixture();UUID orderId=UUID.randomUUID();ShipmentReservation reservation=new ShipmentReservation(orderId,"DEFAULT","pay-42");when(f.reservations.findByOrderId(orderId)).thenReturn(Optional.of(reservation));
    f.participant.handle(new ShipmentCommand(UUID.randomUUID(),orderId,"RESERVE_SHIPMENT","pay-42","DEFAULT"));
    assertThat(f.captureEvent().getEventType()).isEqualTo("SHIPMENT_RESERVED");verifyNoInteractions(f.capacity);
  }

  @Test void duplicateMessageHasNoSideEffects(){
    Fixture f=new Fixture();UUID messageId=UUID.randomUUID();when(f.receipts.existsById(messageId)).thenReturn(true);
    f.participant.handle(new ShipmentCommand(messageId,UUID.randomUUID(),"RESERVE_SHIPMENT","pay-42","DEFAULT"));
    verifyNoInteractions(f.capacity,f.reservations,f.outbox);
  }

  static class Fixture {
    final ShipmentCapacityRepository capacity=mock(ShipmentCapacityRepository.class);final ShipmentReservationRepository reservations=mock(ShipmentReservationRepository.class);final ShipmentCommandReceiptRepository receipts=mock(ShipmentCommandReceiptRepository.class);final OutboxRepository outbox=mock(OutboxRepository.class);
    final ShipmentParticipant participant=new ShipmentParticipant(capacity,reservations,receipts,outbox,new ObjectMapper(),"DEFAULT");
    OutboxEvent captureEvent(){ArgumentCaptor<OutboxEvent> c=ArgumentCaptor.forClass(OutboxEvent.class);verify(outbox).save(c.capture());return c.getValue();}
  }
}
