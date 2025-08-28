
package com.shared.moneytime.starter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shared")
public class SharedMoneyTimeProperties {

  private final Money money = new Money();
  private final Time time = new Time();

  public Money getMoney() { return money; }
  public Time getTime() { return time; }

  public static class Money {
    private String defaultCurrency = "USD";
    private String locale = "en-US";
    private String roundingMode = "HALF_EVEN";
    private String format = "AMOUNT_CURRENCY";
    public String getDefaultCurrency() { return defaultCurrency; }
    public void setDefaultCurrency(String defaultCurrency) { this.defaultCurrency = defaultCurrency; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public String getRoundingMode() { return roundingMode; }
    public void setRoundingMode(String roundingMode) { this.roundingMode = roundingMode; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
  }

  public static class Time {
    private String zone = "UTC";
    private String businessDays = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY";
    private String workStart = "09:00";
    private String workEnd = "17:00";
    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }
    public String getBusinessDays() { return businessDays; }
    public void setBusinessDays(String businessDays) { this.businessDays = businessDays; }
    public String getWorkStart() { return workStart; }
    public void setWorkStart(String workStart) { this.workStart = workStart; }
    public String getWorkEnd() { return workEnd; }
    public void setWorkEnd(String workEnd) { this.workEnd = workEnd; }
  }
}
