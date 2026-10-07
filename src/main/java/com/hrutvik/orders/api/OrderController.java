package com.hrutvik.orders.api;

import com.hrutvik.orders.application.OrderService;
import com.hrutvik.orders.domain.FulfillmentOrder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@RestController @RequestMapping("/v1/orders")
public class OrderController {
  private final OrderService service;
  public OrderController(OrderService service){this.service=service;}
  @PostMapping ResponseEntity<Response> create(Authentication identity,@Valid @RequestBody Request request){return ResponseEntity.status(HttpStatus.CREATED).body(Response.from(service.create(identity.getName(),request.clientOrderId(),request.amount(),request.currency(),request.sku(),request.quantity())));}
  @GetMapping("/{id}") Response get(Authentication identity,@PathVariable UUID id){return service.find(id,identity.getName()).map(Response::from).orElseThrow(()->new OrderNotFoundException(id));}
  public record Request(@NotBlank @Size(max=100) String clientOrderId,@NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal amount,@NotBlank @Pattern(regexp="[A-Za-z]{3}") String currency,@NotBlank @Size(max=100) @Pattern(regexp="[A-Za-z0-9._:-]+") String sku,@Min(1) int quantity){}
  public record Response(UUID id,String merchantId,String clientOrderId,BigDecimal amount,String currency,String sku,int quantity,String status,String failureCode,long version,Instant createdAt,Instant updatedAt){
    static Response from(FulfillmentOrder o){return new Response(o.getId(),o.getMerchantId(),o.getClientOrderId(),o.getAmount(),o.getCurrency(),o.getSku(),o.getQuantity(),o.getStatus().name(),o.getFailureCode(),o.getVersion(),o.getCreatedAt(),o.getUpdatedAt());}
  }
}
