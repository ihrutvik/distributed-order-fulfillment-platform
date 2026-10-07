package com.hrutvik.orders.shipment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ShipmentCommandReceiptRepository extends JpaRepository<ShipmentCommandReceipt,UUID> {}
