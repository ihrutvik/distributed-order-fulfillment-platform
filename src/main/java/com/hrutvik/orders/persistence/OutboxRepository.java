package com.hrutvik.orders.persistence;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface OutboxRepository extends JpaRepository<OutboxEvent,UUID> {
  @Query(value="select * from outbox_events where published_at is null order by created_at limit :limit for update skip locked",nativeQuery=true)
  List<OutboxEvent> lockNextBatch(@Param("limit") int limit);
}
