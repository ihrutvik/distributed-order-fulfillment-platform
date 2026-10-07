package com.hrutvik.orders.persistence;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface SagaDeadlineRepository extends JpaRepository<SagaDeadline,UUID> {
  Optional<SagaDeadline> findByOrderId(UUID orderId);
  @Query(value="select * from saga_deadlines where completed=false and next_attempt_at<=:now order by next_attempt_at limit :limit for update skip locked",nativeQuery=true)
  List<SagaDeadline> lockDue(@Param("now") Instant now,@Param("limit") int limit);
}
