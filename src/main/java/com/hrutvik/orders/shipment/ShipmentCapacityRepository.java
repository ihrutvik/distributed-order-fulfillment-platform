package com.hrutvik.orders.shipment;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ShipmentCapacityRepository extends JpaRepository<ShipmentCapacity,String> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select capacity from ShipmentCapacity capacity where capacity.zone=:zone")
  Optional<ShipmentCapacity> lockByZone(@Param("zone") String zone);
}
