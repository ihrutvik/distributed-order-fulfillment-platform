package com.hrutvik.orders.security;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity @Table(name="api_credentials",uniqueConstraints=@UniqueConstraint(name="uk_api_credential_hash",columnNames="key_hash"))
public class ApiCredential {
  @Id private UUID id;
  @Column(name="merchant_id",nullable=false,length=100) private String merchantId;
  @Column(name="key_hash",nullable=false,length=64) private String keyHash;
  @Column(nullable=false,length=255) private String scopes;
  @Column(nullable=false) private boolean active;
  @Column(name="created_at",nullable=false) private Instant createdAt;
  protected ApiCredential(){}
  public ApiCredential(UUID id,String merchantId,String keyHash,String scopes,boolean active){this.id=id;this.merchantId=merchantId;this.keyHash=keyHash;this.scopes=scopes;this.active=active;this.createdAt=Instant.now();}
  public String getMerchantId(){return merchantId;} public String getKeyHash(){return keyHash;} public boolean isActive(){return active;}
  public Set<String> getScopes(){return Set.copyOf(Arrays.asList(scopes.split(",")));}
}
