package com.hrutvik.orders.security;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ApiCredentialRepository extends JpaRepository<ApiCredential,UUID> {
  Optional<ApiCredential> findByKeyHashAndActiveTrue(String keyHash);
}
