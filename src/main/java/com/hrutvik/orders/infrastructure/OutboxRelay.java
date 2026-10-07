package com.hrutvik.orders.infrastructure;

import com.hrutvik.orders.persistence.OutboxRepository;
import com.hrutvik.orders.observability.OrderMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxRelay {
  private final OutboxRepository outbox; private final KafkaTemplate<String,String> kafka; private final OrderMetrics metrics; private final String lifecycleTopic; private final String inventoryTopic; private final String paymentTopic; private final String shipmentTopic; private final String repliesTopic; private final int batchSize;
  public OutboxRelay(OutboxRepository outbox,KafkaTemplate<String,String> kafka,OrderMetrics metrics,@Value("${orders.events.topic}") String lifecycleTopic,@Value("${orders.saga.inventory-commands-topic}") String inventoryTopic,@Value("${orders.saga.payment-commands-topic}") String paymentTopic,@Value("${orders.saga.shipment-commands-topic}") String shipmentTopic,@Value("${orders.saga.replies-topic}") String repliesTopic,@Value("${orders.outbox.batch-size:100}") int batchSize){this.outbox=outbox;this.kafka=kafka;this.metrics=metrics;this.lifecycleTopic=lifecycleTopic;this.inventoryTopic=inventoryTopic;this.paymentTopic=paymentTopic;this.shipmentTopic=shipmentTopic;this.repliesTopic=repliesTopic;this.batchSize=batchSize;}
  @Scheduled(fixedDelayString="${orders.outbox.poll-delay:1s}") @Transactional
  public void publish(){for(var event:outbox.lockNextBatch(batchSize)){kafka.send(topic(event.getDestination()),event.getAggregateId().toString(),event.getPayload()).join();event.published();metrics.outboxPublished(event.getDestination());}}
  private String topic(String destination){return switch(destination){case "INVENTORY_COMMANDS"->inventoryTopic;case "PAYMENT_COMMANDS"->paymentTopic;case "SHIPMENT_COMMANDS"->shipmentTopic;case "SAGA_REPLIES"->repliesTopic;default->lifecycleTopic;};}
}
