package com.hrutvik.orders.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface InventoryCommandReceiptRepository extends JpaRepository<InventoryCommandReceipt,UUID> {}
