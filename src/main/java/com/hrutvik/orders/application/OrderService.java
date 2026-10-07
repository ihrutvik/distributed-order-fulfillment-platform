package com.hrutvik.orders.application;

import com.hrutvik.orders.domain.FulfillmentOrder;
import com.hrutvik.orders.api.IdempotencyConflictException;
import com.hrutvik.orders.persistence.*;
import com.hrutvik.orders.observability.OrderMetrics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class OrderService {
  private final FulfillmentOrderRepository orders; private final OutboxRepository outbox; private final SagaDeadlineRepository deadlines; private final OrderMetrics metrics;
  public OrderService(FulfillmentOrderRepository orders,OutboxRepository outbox,SagaDeadlineRepository deadlines,OrderMetrics metrics){this.orders=orders;this.outbox=outbox;this.deadlines=deadlines;this.metrics=metrics;}
  @Transactional
  public FulfillmentOrder create(String merchantId,String clientOrderId,BigDecimal amount,String currency){
    return create(merchantId,clientOrderId,amount,currency,"DEMO-SKU",1);
  }
  @Transactional
  public FulfillmentOrder create(String merchantId,String clientOrderId,BigDecimal amount,String currency,String sku,int quantity){
    String normalizedSku=sku.strip().toUpperCase(Locale.ROOT);
    String fingerprint=OrderRequestFingerprint.calculate(merchantId,clientOrderId,amount,currency,normalizedSku,quantity);
    var existing=orders.findByMerchantIdAndClientOrderId(merchantId,clientOrderId);
    if(existing.isPresent()){
      if(existing.get().getRequestFingerprint()!=null && !existing.get().getRequestFingerprint().equals(fingerprint)){metrics.orderRequest("conflict");throw new IdempotencyConflictException(clientOrderId);}
      metrics.orderRequest("replay");
      return existing.get();
    }
    FulfillmentOrder order=orders.save(new FulfillmentOrder(UUID.randomUUID(),merchantId,clientOrderId,amount,currency,normalizedSku,quantity,fingerprint));
    UUID messageId=UUID.randomUUID();
    String payload="{\"messageId\":\""+messageId+"\",\"orderId\":\""+order.getId()+"\",\"type\":\"RESERVE_INVENTORY\",\"sku\":\""+normalizedSku+"\",\"quantity\":"+quantity+"}";
    outbox.save(new OutboxEvent(order.getId(),"RESERVE_INVENTORY",payload,"INVENTORY_COMMANDS"));
    deadlines.save(new SagaDeadline(order.getId(),"RESERVE_INVENTORY",payload,"INVENTORY_COMMANDS",Instant.now().plusSeconds(30)));
    metrics.orderRequest("created");
    return order;
  }
  @Transactional(readOnly=true) public Optional<FulfillmentOrder> find(UUID id){return orders.findById(id);}
  @Transactional(readOnly=true) public Optional<FulfillmentOrder> find(UUID id,String merchantId){return orders.findByIdAndMerchantId(id,merchantId);}
}
