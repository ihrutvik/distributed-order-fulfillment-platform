package com.hrutvik.orders.security;

import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

@Service
public class ApiKeyAuthenticator {
  private final ApiCredentialRepository credentials;
  public ApiKeyAuthenticator(ApiCredentialRepository credentials){this.credentials=credentials;}
  public Optional<Identity> authenticate(String rawKey){
    if(rawKey==null || rawKey.isBlank())return Optional.empty();
    byte[] digest=digest(rawKey);String hash=HexFormat.of().formatHex(digest);
    return credentials.findByKeyHashAndActiveTrue(hash).filter(c->MessageDigest.isEqual(digest,HexFormat.of().parseHex(c.getKeyHash()))).map(c->new Identity(c.getMerchantId(),c.getScopes()));
  }
  private byte[] digest(String value){try{return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));}catch(NoSuchAlgorithmException e){throw new IllegalStateException("SHA-256 unavailable",e);}}
  public record Identity(String merchantId,Set<String> scopes){}
}
