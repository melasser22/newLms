package com.ejada.push.usage.model;

public class UsageRecord {
  private long sent;
  private long delivered;
  private long opened;

  public void incrementSent() {
    sent++;
  }

  public void incrementDelivered() {
    delivered++;
  }

  public void incrementOpened() {
    opened++;
  }

  public long getSent() {
    return sent;
  }

  public long getDelivered() {
    return delivered;
  }

  public long getOpened() {
    return opened;
  }
}
