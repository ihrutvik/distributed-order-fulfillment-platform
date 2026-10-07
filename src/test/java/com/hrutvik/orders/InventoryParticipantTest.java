package com.hrutvik.orders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrutvik.orders.inventory.*;
import com.hrutvik.orders.persistence.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InventoryParticipantTest {
  @Test void reservesStockAndEmitsDurableSagaReply(){
    Fixture f=new Fixture();InventoryStock item=new InventoryStock("PHONE-1",5);when(f.stock.lockBySku("PHONE-1")).thenReturn(Optional.of(item));when(f.reservations.save(any())).thenAnswer(i->i.getArgument(0));
    f.participant.handle(new InventoryCommand(UUID.randomUUID(),UUID.randomUUID(),"RESERVE_INVENTORY","PHONE-1",2,null));
    assertThat(item.getAvailable()).isEqualTo(3);assertThat(item.getReserved()).isEqualTo(2);
    OutboxEvent event=f.captureEvent();assertThat(event.getEventType()).isEqualTo("INVENTORY_RESERVED");assertThat(event.getDestination()).isEqualTo("SAGA_REPLIES");verify(f.receipts).save(any());
  }

  @Test void rejectsReservationWithoutMutatingInsufficientStock(){
    Fixture f=new Fixture();InventoryStock item=new InventoryStock("PHONE-1",1);when(f.stock.lockBySku("PHONE-1")).thenReturn(Optional.of(item));
    f.participant.handle(new InventoryCommand(UUID.randomUUID(),UUID.randomUUID(),"RESERVE_INVENTORY","PHONE-1",2,null));
    assertThat(item.getAvailable()).isEqualTo(1);assertThat(f.captureEvent().getEventType()).isEqualTo("INVENTORY_REJECTED");verify(f.reservations,never()).save(any());
  }

  @Test void duplicateCommandHasNoSideEffects(){
    Fixture f=new Fixture();UUID messageId=UUID.randomUUID();when(f.receipts.existsById(messageId)).thenReturn(true);
    f.participant.handle(new InventoryCommand(messageId,UUID.randomUUID(),"RESERVE_INVENTORY","PHONE-1",1,null));
    verifyNoInteractions(f.stock,f.reservations,f.outbox);
  }

  static class Fixture {
    final InventoryStockRepository stock=mock(InventoryStockRepository.class);final InventoryReservationRepository reservations=mock(InventoryReservationRepository.class);final InventoryCommandReceiptRepository receipts=mock(InventoryCommandReceiptRepository.class);final OutboxRepository outbox=mock(OutboxRepository.class);
    final InventoryParticipant participant=new InventoryParticipant(stock,reservations,receipts,outbox,new ObjectMapper());
    OutboxEvent captureEvent(){ArgumentCaptor<OutboxEvent> c=ArgumentCaptor.forClass(OutboxEvent.class);verify(outbox).save(c.capture());return c.getValue();}
  }
}
