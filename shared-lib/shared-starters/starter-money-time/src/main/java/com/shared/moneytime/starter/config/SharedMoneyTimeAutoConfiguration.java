package com.shared.moneytime.starter.config;

import com.fasterxml.jackson.databind.Module;
import com.shared.moneytime.starter.jackson.SharedMoneyModule;
import com.shared.moneytime.starter.time.TimeService;
import com.shared.moneytime.starter.web.MonetaryAmountFormatter;
import com.shared.moneytime.starter.web.InstantFormatter;
import com.shared.moneytime.starter.web.DurationFormatter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.format.MonetaryAmountFormat;
import javax.money.format.MonetaryFormats;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Locale;

@AutoConfiguration
@EnableConfigurationProperties(SharedMoneyTimeProperties.class)
@PropertySource("classpath:/com/shared/moneytime/starter/money-time-defaults.properties")
public class SharedMoneyTimeAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public CurrencyUnit defaultCurrency(SharedMoneyTimeProperties props) {
    return Monetary.getCurrency(props.getMoney().getDefaultCurrency());
  }

  @Bean
  @ConditionalOnMissingBean
  public MonetaryAmountFormat monetaryAmountFormat(SharedMoneyTimeProperties props) {
    Locale locale = Locale.forLanguageTag(props.getMoney().getLocale());
    return MonetaryFormats.getAmountFormat(locale);
  }

  @Bean
  @ConditionalOnMissingBean
  public Clock clock(SharedMoneyTimeProperties props) {
    return Clock.system(ZoneId.of(props.getTime().getZone()));
  }

  @Bean
  @ConditionalOnMissingBean
  public TimeService timeService(Clock clock, SharedMoneyTimeProperties props) {
    return new TimeService(clock, props.getTime().getZone(),
        props.getTime().getBusinessDays(), props.getTime().getWorkStart(), props.getTime().getWorkEnd());
  }

  @Bean
  @ConditionalOnClass(name = "com.fasterxml.jackson.databind.ObjectMapper")
  @ConditionalOnMissingBean(name = "sharedMoneyModule")
  public Module sharedMoneyModule() {
    return new SharedMoneyModule();
  }

  @Bean
  @ConditionalOnMissingBean
  public MonetaryAmountFormatter monetaryAmountFormatter(MonetaryAmountFormat format) {
    return new MonetaryAmountFormatter(format);
  }

  @Bean @ConditionalOnMissingBean public InstantFormatter instantFormatter() { return new InstantFormatter(); }
  @Bean @ConditionalOnMissingBean public DurationFormatter durationFormatter() { return new DurationFormatter(); }
}
