package com.hrutvik.orders.shipment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ShipmentReservationRepository extends JpaRepository<ShipmentReservation,UUID> {
  Optional<ShipmentReservation> findByOrderId(UUID orderId);
}
