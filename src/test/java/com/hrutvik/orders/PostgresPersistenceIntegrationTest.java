package com.hrutvik.orders;

import com.hrutvik.orders.application.OrderRequestFingerprint;
import com.hrutvik.orders.domain.FulfillmentOrder;
import com.hrutvik.orders.persistence.FulfillmentOrderRepository;
import com.hrutvik.orders.persistence.InboxMessage;
import com.hrutvik.orders.persistence.InboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker=true)
class PostgresPersistenceIntegrationTest {
  @Container static final PostgreSQLContainer<?> POSTGRES=new PostgreSQLContainer<>("postgres:16-alpine");
  @DynamicPropertySource static void database(DynamicPropertyRegistry registry){
    registry.add("spring.datasource.url",POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username",POSTGRES::getUsername);
    registry.add("spring.datasource.password",POSTGRES::getPassword);
  }

  @Autowired FulfillmentOrderRepository orders;
  @Autowired InboxRepository inbox;
  @Autowired JdbcTemplate jdbc;

  @Test void flywaySchemaEnforcesMerchantScopedOrderIdempotency(){
    String hash=OrderRequestFingerprint.calculate("merchant-1","checkout-1",new BigDecimal("12.00"),"USD");
    orders.saveAndFlush(new FulfillmentOrder(UUID.randomUUID(),"merchant-1","checkout-1",new BigDecimal("12.00"),"USD",hash));
    assertThatThrownBy(()->orders.saveAndFlush(new FulfillmentOrder(UUID.randomUUID(),"merchant-1","checkout-1",new BigDecimal("12.00"),"USD",hash))).isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test void inboxPrimaryKeyRejectsDuplicateKafkaDelivery(){
    UUID messageId=UUID.randomUUID();UUID orderId=UUID.randomUUID();
    inbox.saveAndFlush(new InboxMessage(messageId,orderId,"INVENTORY_RESERVED"));
    assertThat(inbox.existsById(messageId)).isTrue();
    assertThatThrownBy(()->jdbc.update("insert into inbox_messages(message_id,order_id,event_type,handled_at) values (?,?,?,now())",messageId,orderId,"INVENTORY_RESERVED")).isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test void orderLookupIsRestrictedToOwningMerchant(){
    UUID id=UUID.randomUUID();String hash=OrderRequestFingerprint.calculate("merchant-1","checkout-owned",new BigDecimal("12.00"),"USD");
    orders.saveAndFlush(new FulfillmentOrder(id,"merchant-1","checkout-owned",new BigDecimal("12.00"),"USD",hash));
    assertThat(orders.findByIdAndMerchantId(id,"merchant-1")).isPresent();assertThat(orders.findByIdAndMerchantId(id,"merchant-2")).isEmpty();
  }
}
