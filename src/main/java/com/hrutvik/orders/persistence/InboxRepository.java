package com.hrutvik.orders.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface InboxRepository extends JpaRepository<InboxMessage,UUID> {}
