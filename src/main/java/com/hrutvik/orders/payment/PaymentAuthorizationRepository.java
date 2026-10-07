package com.hrutvik.orders.payment;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface PaymentAuthorizationRepository extends JpaRepository<PaymentAuthorization,UUID> {
  Optional<PaymentAuthorization> findByOrderId(UUID orderId);
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select authorization from PaymentAuthorization authorization where authorization.orderId=:orderId")
  Optional<PaymentAuthorization> lockByOrderId(@Param("orderId") UUID orderId);
}
