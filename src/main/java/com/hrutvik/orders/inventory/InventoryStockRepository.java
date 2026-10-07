package com.hrutvik.orders.inventory;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface InventoryStockRepository extends JpaRepository<InventoryStock,String> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select stock from InventoryStock stock where stock.sku=:sku")
  Optional<InventoryStock> lockBySku(@Param("sku") String sku);
}
