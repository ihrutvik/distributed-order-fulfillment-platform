package com.hrutvik.orders;

import com.hrutvik.orders.security.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ApiKeyAuthenticatorTest {
  private static final String HASH="cd577fe2561ebff23505db0bb006300c7cdecbd46bc0e03c449afafaca2c25bf";

  @Test void authenticatesHashedKeyAndReturnsMerchantScopes(){
    var repository=mock(ApiCredentialRepository.class);when(repository.findByKeyHashAndActiveTrue(HASH)).thenReturn(Optional.of(new ApiCredential(UUID.randomUUID(),"merchant-42",HASH,"ORDERS_READ,ORDERS_WRITE",true)));
    var identity=new ApiKeyAuthenticator(repository).authenticate("demo-secret");
    assertThat(identity).isPresent();assertThat(identity.get().merchantId()).isEqualTo("merchant-42");assertThat(identity.get().scopes()).containsExactlyInAnyOrder("ORDERS_READ","ORDERS_WRITE");
  }

  @Test void rejectsMissingAndUnknownKeys(){
    var repository=mock(ApiCredentialRepository.class);var authenticator=new ApiKeyAuthenticator(repository);
    assertThat(authenticator.authenticate(null)).isEmpty();assertThat(authenticator.authenticate("wrong-secret")).isEmpty();
  }
}
