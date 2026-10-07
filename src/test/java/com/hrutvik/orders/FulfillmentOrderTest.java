package com.hrutvik.orders;

import com.hrutvik.orders.domain.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class FulfillmentOrderTest {
  @Test void completesHappyPathInStrictOrder(){
    var order=order();order.inventoryReserved("inv-1");order.paymentAuthorized("pay-1");order.shipmentReserved("ship-1");order.confirm();
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);assertThat(order.getShipmentReservationId()).isEqualTo("ship-1");
  }
  @Test void rejectsOutOfOrderTransition(){assertThatThrownBy(()->order().paymentAuthorized("pay-1")).isInstanceOf(IllegalStateException.class).hasMessageContaining("PENDING");}
  @Test void recordsFailureBeforeCompensation(){var order=order();order.inventoryReserved("inv-1");order.beginCompensation("PAYMENT_DECLINED");order.cancel();assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);assertThat(order.getFailureCode()).isEqualTo("PAYMENT_DECLINED");}
  @Test void terminalOrderCannotBeCompensated(){var order=order();order.beginCompensation("OUT_OF_STOCK");order.cancel();assertThatThrownBy(()->order.beginCompensation("LATE_FAILURE")).isInstanceOf(IllegalStateException.class);}
  private FulfillmentOrder order(){return new FulfillmentOrder(UUID.randomUUID(),"merchant-1","checkout-1",new BigDecimal("49.99"),"eur");}
}
