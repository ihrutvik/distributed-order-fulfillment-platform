package com.hrutvik.orders.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PaymentCommandReceiptRepository extends JpaRepository<PaymentCommandReceipt,UUID> {}
