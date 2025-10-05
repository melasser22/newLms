package com.ejada.gateway.config;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Customises the Jasypt encryptor so that multi-tenant deployments can rely on
 * a strong, pooled cipher configuration. The encryptor intentionally fails fast
 * when the password is missing to avoid silently booting without the ability to
 * decrypt shared secrets.
 */
@Configuration
@EnableConfigurationProperties(TenantJasyptProperties.class)
public class MultiTenantJasyptConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(MultiTenantJasyptConfiguration.class);

  @Bean(name = "jasyptStringEncryptor")
  @ConditionalOnMissingBean(name = "jasyptStringEncryptor")
  public StringEncryptor jasyptStringEncryptor(TenantJasyptProperties properties) {
    if (!StringUtils.hasText(properties.getPassword())) {
      throw new IllegalStateException(
          "Jasypt encryption password is not configured. "
              + "Set the JASYPT_ENCRYPTOR_PASSWORD environment variable or the "
              + "jasypt.encryptor.password property before starting the API Gateway.");
    }

    LOGGER.info("Initialising Jasypt encryptor with algorithm '{}' and pool size {}", properties.getAlgorithm(),
        properties.getPoolSize());

    PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
    encryptor.setConfig(createConfig(properties));
    return encryptor;
  }

  private SimpleStringPBEConfig createConfig(TenantJasyptProperties properties) {
    SimpleStringPBEConfig config = new SimpleStringPBEConfig();
    config.setPassword(properties.getPassword());
    config.setAlgorithm(properties.getAlgorithm());
    config.setKeyObtentionIterations(Integer.toString(properties.getKeyObtentionIterations()));
    config.setPoolSize(Integer.toString(properties.getPoolSize()));

    if (StringUtils.hasText(properties.getProviderName())) {
      config.setProviderName(properties.getProviderName());
    }

    config.setSaltGeneratorClassName(properties.getSaltGeneratorClassname());
    config.setIvGeneratorClassName(properties.getIvGeneratorClassname());
    config.setStringOutputType(properties.getStringOutputType());
    return config;
  }
}

