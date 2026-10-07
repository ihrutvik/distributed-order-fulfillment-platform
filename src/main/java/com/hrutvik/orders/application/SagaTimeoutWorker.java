package com.hrutvik.orders.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrutvik.orders.persistence.*;
import com.hrutvik.orders.observability.OrderMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.UUID;

@Component
public class SagaTimeoutWorker {
  private final SagaDeadlineRepository deadlines; private final OutboxRepository outbox; private final ObjectMapper json; private final OrderMetrics metrics; private final int maxAttempts; private final Duration baseBackoff; private final int batchSize;
  public SagaTimeoutWorker(SagaDeadlineRepository deadlines,OutboxRepository outbox,ObjectMapper json,OrderMetrics metrics,@Value("${orders.saga.timeout-max-attempts:3}") int maxAttempts,@Value("${orders.saga.timeout-base-backoff:10s}") Duration baseBackoff,@Value("${orders.saga.timeout-batch-size:50}") int batchSize){this.deadlines=deadlines;this.outbox=outbox;this.json=json;this.metrics=metrics;this.maxAttempts=maxAttempts;this.baseBackoff=baseBackoff;this.batchSize=batchSize;}

  @Scheduled(fixedDelayString="${orders.saga.timeout-poll-delay:5s}")
  @Transactional
  public void retryDue(){Instant now=Instant.now();for(var deadline:deadlines.lockDue(now,batchSize)){if(deadline.getAttempts()>=maxAttempts)exhaust(deadline);else retry(deadline,now);}}

  private void retry(SagaDeadline deadline,Instant now){
    outbox.save(new OutboxEvent(deadline.getOrderId(),deadline.getStage(),deadline.getPayload(),deadline.getDestination()));
    long multiplier=1L<<Math.min(deadline.getAttempts(),20);deadline.retried(now.plus(baseBackoff.multipliedBy(multiplier)));
    metrics.sagaTimeout(deadline.getStage(),"retried");
  }

  private void exhaust(SagaDeadline deadline){
    String rejection=switch(deadline.getStage()){case "RESERVE_INVENTORY"->"INVENTORY_REJECTED";case "AUTHORIZE_PAYMENT"->"PAYMENT_DECLINED";case "RESERVE_SHIPMENT"->"SHIPMENT_REJECTED";default->null;};
    if(rejection==null){outbox.save(new OutboxEvent(deadline.getOrderId(),"COMPENSATION_STALLED","{\"orderId\":\""+deadline.getOrderId()+"\",\"stage\":\""+deadline.getStage()+"\",\"reason\":\"SAGA_TIMEOUT_EXHAUSTED\"}"));}
    else try{SagaReply reply=new SagaReply(UUID.randomUUID(),deadline.getOrderId(),rejection,null,"SAGA_TIMEOUT_EXHAUSTED");outbox.save(new OutboxEvent(deadline.getOrderId(),rejection,json.writeValueAsString(reply),"SAGA_REPLIES"));}
    catch(JsonProcessingException e){throw new IllegalStateException("Could not serialize timeout reply",e);}
    deadline.complete();
    metrics.sagaTimeout(deadline.getStage(),"exhausted");
  }
}
