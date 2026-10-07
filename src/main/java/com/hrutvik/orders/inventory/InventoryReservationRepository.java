package com.hrutvik.orders.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation,UUID> {
  Optional<InventoryReservation> findByOrderId(UUID orderId);
}
