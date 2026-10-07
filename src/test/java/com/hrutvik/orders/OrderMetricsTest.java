package com.hrutvik.orders;

import com.hrutvik.orders.observability.OrderMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class OrderMetricsTest {
  @Test void recordsLowCardinalityOperationalOutcomes(){
    var registry=new SimpleMeterRegistry();var metrics=new OrderMetrics(registry);
    metrics.orderRequest("created");metrics.sagaReply("PAYMENT_AUTHORIZED","applied");metrics.sagaTimeout("RESERVE_SHIPMENT","retried");metrics.outboxPublished("PAYMENT_COMMANDS");
    assertThat(registry.get("orders.requests").tag("outcome","created").counter().count()).isEqualTo(1);
    assertThat(registry.get("orders.saga.replies").tag("type","PAYMENT_AUTHORIZED").tag("outcome","applied").counter().count()).isEqualTo(1);
    assertThat(registry.get("orders.saga.timeouts").tag("stage","RESERVE_SHIPMENT").tag("outcome","retried").counter().count()).isEqualTo(1);
    assertThat(registry.get("orders.outbox.published").tag("destination","PAYMENT_COMMANDS").counter().count()).isEqualTo(1);
  }
}
