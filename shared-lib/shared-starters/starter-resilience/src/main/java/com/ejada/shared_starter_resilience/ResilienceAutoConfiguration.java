package com.ejada.shared_starter_resilience;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@AutoConfiguration
@EnableConfigurationProperties(SharedResilienceProps.class)
public class ResilienceAutoConfiguration {

  @Bean
  @ConditionalOnClass({WebClient.class, HttpClient.class, ReactorClientHttpConnector.class})
  @ConditionalOnMissingBean(WebClient.Builder.class)
  public WebClient.Builder resilientWebClientBuilder(SharedResilienceProps props) {
    HttpClient client = HttpClient.create()
        .responseTimeout(Duration.ofMillis(props.getHttpTimeoutMs()))
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.getConnectTimeoutMs());

    return WebClient.builder().clientConnector(new ReactorClientHttpConnector(client));
  }
}
