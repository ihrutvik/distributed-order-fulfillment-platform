package com.hrutvik.orders.persistence;

import com.hrutvik.orders.domain.FulfillmentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface FulfillmentOrderRepository extends JpaRepository<FulfillmentOrder,UUID> {
  Optional<FulfillmentOrder> findByMerchantIdAndClientOrderId(String merchantId,String clientOrderId);
  Optional<FulfillmentOrder> findByIdAndMerchantId(UUID id,String merchantId);
}
