package com.shared.shared_starter_ratelimit;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.data.redis.core.StringRedisTemplate;
@AutoConfiguration @EnableConfigurationProperties(RateLimitProps.class)
public class RateLimitAutoConfiguration {
  @Bean
  public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(StringRedisTemplate redis, RateLimitProps props){
    FilterRegistrationBean<RateLimitFilter> reg = new FilterRegistrationBean<>();
    reg.setFilter(new RateLimitFilter(redis, props));
    reg.addUrlPatterns("/api/*"); reg.setOrder(1);
    return reg;
  }
}
