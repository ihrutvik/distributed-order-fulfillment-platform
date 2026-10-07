package com.hrutvik.orders.inventory;

import jakarta.persistence.*;

@Entity @Table(name="inventory_stock")
public class InventoryStock {
  @Id @Column(length=100) private String sku;
  @Column(nullable=false) private int available;
  @Column(nullable=false) private int reserved;
  @Version private long version;
  protected InventoryStock(){}
  public InventoryStock(String sku,int available){this.sku=sku;this.available=available;}
  public boolean reserve(int quantity){if(quantity<1) throw new IllegalArgumentException("Quantity must be positive");if(available<quantity)return false;available-=quantity;reserved+=quantity;return true;}
  public void release(int quantity){if(quantity<1 || reserved<quantity) throw new IllegalStateException("Cannot release unreserved stock");reserved-=quantity;available+=quantity;}
  public String getSku(){return sku;} public int getAvailable(){return available;} public int getReserved(){return reserved;}
}
