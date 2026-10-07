package com.hrutvik.orders.shipment;

import jakarta.persistence.*;

@Entity @Table(name="shipment_capacity")
public class ShipmentCapacity {
  @Id @Column(length=50) private String zone;
  @Column(name="available_slots",nullable=false) private int availableSlots;
  @Version private long version;
  protected ShipmentCapacity(){}
  public ShipmentCapacity(String zone,int availableSlots){this.zone=zone;this.availableSlots=availableSlots;}
  public boolean reserve(){if(availableSlots<1)return false;availableSlots--;return true;}
  public String getZone(){return zone;} public int getAvailableSlots(){return availableSlots;}
}
