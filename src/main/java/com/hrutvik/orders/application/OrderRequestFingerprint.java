package com.hrutvik.orders.application;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

public final class OrderRequestFingerprint {
  private OrderRequestFingerprint() {}

  public static String calculate(String merchantId,String clientOrderId,BigDecimal amount,String currency){
    return calculate(merchantId,clientOrderId,amount,currency,"DEMO-SKU",1);
  }
  public static String calculate(String merchantId,String clientOrderId,BigDecimal amount,String currency,String sku,int quantity){
    String canonical=merchantId.strip()+"\n"+clientOrderId.strip()+"\n"+amount.stripTrailingZeros().toPlainString()+"\n"+currency.strip().toUpperCase(Locale.ROOT)+"\n"+sku.strip().toUpperCase(Locale.ROOT)+"\n"+quantity;
    try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));}
    catch(NoSuchAlgorithmException e){throw new IllegalStateException("SHA-256 is unavailable",e);}
  }
}
