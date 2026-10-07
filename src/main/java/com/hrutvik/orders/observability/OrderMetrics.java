package com.hrutvik.orders.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class OrderMetrics {
  private final MeterRegistry registry;
  public OrderMetrics(MeterRegistry registry){this.registry=registry;}
  public void orderRequest(String outcome){counter("orders.requests","Order API outcomes","outcome",outcome);}
  public void sagaReply(String type,String outcome){counter("orders.saga.replies","Saga reply processing outcomes","type",type,"outcome",outcome);}
  public void sagaTimeout(String stage,String outcome){counter("orders.saga.timeouts","Saga deadline outcomes","stage",stage,"outcome",outcome);}
  public void outboxPublished(String destination){counter("orders.outbox.published","Published transactional outbox messages","destination",destination);}
  private void counter(String name,String description,String... tags){Counter.builder(name).description(description).tags(tags).register(registry).increment();}
}
