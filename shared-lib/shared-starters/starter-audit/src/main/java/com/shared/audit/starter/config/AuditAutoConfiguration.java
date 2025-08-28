package com.shared.audit.starter.config;

import com.shared.audit.starter.api.AuditService;
import com.shared.audit.starter.api.DefaultTenantProvider;
import com.shared.audit.starter.api.ReactiveAuditService;
import com.shared.audit.starter.api.TenantProvider;
import com.shared.audit.starter.core.DefaultAuditService;
import com.shared.audit.starter.core.DefaultReactiveAuditService;
import com.shared.audit.starter.core.dispatch.AuditDispatcher;
import com.shared.audit.starter.core.dispatch.sinks.DatabaseSink;
import com.shared.audit.starter.core.dispatch.sinks.OutboxSink;
import com.shared.audit.starter.core.dispatch.sinks.Sink;
import com.shared.audit.starter.core.enrich.Enricher;
import com.shared.audit.starter.core.enrich.HostEnricher;
import com.shared.audit.starter.core.enrich.MdcTraceEnricher;
import com.shared.audit.starter.core.enrich.RequestEnricher;
import com.shared.audit.starter.core.enrich.SecurityEnricher;
import com.shared.audit.starter.core.enrich.TenantEnricher;
import com.shared.audit.starter.core.mask.DefaultMaskingStrategy;
import com.shared.audit.starter.core.mask.MaskingStrategy;
import com.shared.audit.starter.http.AuditWebMvcFilter;
import com.shared.audit.starter.metrics.AuditHealthIndicator;
import com.shared.audit.starter.metrics.AuditMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

@AutoConfiguration
@EnableConfigurationProperties(AuditProperties.class)
@ConditionalOnProperty(prefix = "shared.audit", name = "enabled", havingValue = "true")
@AutoConfigureAfter(name = {
    "org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration",
    "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
    "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration",
    "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
})
public class AuditAutoConfiguration {

  // ---------- Core

  @Bean
  @ConditionalOnMissingBean
  public MaskingStrategy maskingStrategy(AuditProperties props) {
    Object masking = props.getMasking();

    // fieldsByKey can be List<String> or Map<String,String> depending on version
    Object fieldsByKeyObj = callGetter(masking, "getFieldsByKey");
    Map<String, String> fieldsMap = new LinkedHashMap<>();
    if (fieldsByKeyObj instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<Object, Object> m = (Map<Object, Object>) fieldsByKeyObj;
      for (Map.Entry<Object, Object> e : m.entrySet()) {
        fieldsMap.put(String.valueOf(e.getKey()), String.valueOf(e.getValue()));
      }
    } else if (fieldsByKeyObj instanceof List) {
      for (Object k : (List<?>) fieldsByKeyObj) {
        fieldsMap.put(String.valueOf(k), "****");
      }
    }

    // optional jsonPaths (may not exist)
    @SuppressWarnings("unchecked")
    List<String> jsonPaths = (List<String>) optionalGetter(masking, "getJsonPaths", List.class, Collections.emptyList());

    // Try constructors in this order:
    // 1) (Map<String,String>, List<String>)
    // 2) (Map<String,String>)
    // 3) (Iterable<String>)
    // 4) (List<String>)  // legacy (if strategy accepted only keys)
    try {
      Constructor<?> c = DefaultMaskingStrategy.class.getConstructor(Map.class, List.class);
      return (MaskingStrategy) c.newInstance(fieldsMap, jsonPaths);
    } catch (NoSuchMethodException ignored) {
    } catch (Exception e) {
      throw new IllegalStateException("Failed to instantiate DefaultMaskingStrategy(Map,List)", e);
    }
    try {
      Constructor<?> c = DefaultMaskingStrategy.class.getConstructor(Map.class);
      return (MaskingStrategy) c.newInstance(fieldsMap);
    } catch (NoSuchMethodException ignored) {
    } catch (Exception e) {
      throw new IllegalStateException("Failed to instantiate DefaultMaskingStrategy(Map)", e);
    }
    try {
      Constructor<?> c = DefaultMaskingStrategy.class.getConstructor(Iterable.class);
      return (MaskingStrategy) c.newInstance(new ArrayList<>(fieldsMap.keySet()));
    } catch (NoSuchMethodException ignored) {
    } catch (Exception e) {
      throw new IllegalStateException("Failed to instantiate DefaultMaskingStrategy(Iterable)", e);
    }
    try {
      Constructor<?> c = DefaultMaskingStrategy.class.getConstructor(List.class);
      return (MaskingStrategy) c.newInstance(new ArrayList<>(fieldsMap.keySet()));
    } catch (Exception e) {
      throw new IllegalStateException("No compatible DefaultMaskingStrategy constructor found", e);
    }
  }

  @Bean
  @ConditionalOnMissingBean
  public AuditDispatcher auditDispatcher(ObjectProvider<Sink> sinks, AuditProperties props) {
    AuditDispatcher dispatcher = new AuditDispatcher(sinks.orderedStream().toList());

    Object dispatcherProps = props.getDispatcher();
    Integer queueCapacity = optionalInt(dispatcherProps, "getQueueCapacity");
    Integer maxDrainBatch = optionalInt(dispatcherProps, "getMaxDrainBatch");
    Integer maxRetries = optionalInt(dispatcherProps, "getMaxRetries");
    Long retryBackoffMs = optionalLong(dispatcherProps, "getRetryBackoffMs");
    Boolean async = optionalBoolean(dispatcherProps, "isAsync");

    invokeSetterIfPresent(dispatcher, "setQueueCapacity", int.class, queueCapacity);
    invokeSetterIfPresent(dispatcher, "setMaxDrainBatch", int.class, maxDrainBatch);
    invokeSetterIfPresent(dispatcher, "setMaxRetries", int.class, maxRetries);
    invokeSetterIfPresent(dispatcher, "setRetryBackoffMs", long.class, retryBackoffMs);
    invokeSetterIfPresent(dispatcher, "setAsync", boolean.class, async);

    return dispatcher;
  }

  @Bean
  @ConditionalOnMissingBean
  public AuditService auditService(AuditDispatcher dispatcher,
                                   ObjectProvider<Enricher> enrichers,
                                   MaskingStrategy masking,
                                   ObjectProvider<AuditMetrics> metrics // optional
  ) {
    return new DefaultAuditService(dispatcher, enrichers.orderedStream().toList(), masking);
  }

  @Bean
  @ConditionalOnMissingBean
  public ReactiveAuditService reactiveAuditService(AuditService auditService) {
    return new DefaultReactiveAuditService(auditService);
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnClass(MeterRegistry.class)
  public AuditMetrics auditMetrics(MeterRegistry registry) {
    return new AuditMetrics(registry);
  }

  @Bean
  public AuditHealthIndicator auditHealthIndicator() {
    return new AuditHealthIndicator();
  }

  @Bean
  @ConditionalOnMissingBean
  public TenantProvider tenantProvider() {
    return new DefaultTenantProvider();
  }

  // ---------- Enrichers

  @Bean @ConditionalOnMissingBean public MdcTraceEnricher mdcTraceEnricher() { return new MdcTraceEnricher(); }
  @Bean @ConditionalOnMissingBean public HostEnricher hostEnricher() { return new HostEnricher(); }

  @Bean
  @ConditionalOnMissingBean
  public TenantEnricher tenantEnricher(TenantProvider provider) {
    return new TenantEnricher(provider::getTenantId);
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnClass(name = "org.springframework.security.core.context.SecurityContextHolder")
  public SecurityEnricher securityEnricher() { return new SecurityEnricher(); }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnClass(name = "org.springframework.web.context.request.RequestContextHolder")
  public RequestEnricher requestEnricher() { return new RequestEnricher(); }

  // ---------- Dedicated TX template for audit writes

  @Bean(name = "auditTransactionTemplate")
  @ConditionalOnBean(PlatformTransactionManager.class)
  @ConditionalOnMissingBean(name = "auditTransactionTemplate")
  public TransactionTemplate auditTransactionTemplate(PlatformTransactionManager ptm) {
    TransactionTemplate t = new TransactionTemplate(ptm);
    t.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    t.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    return t;
  }

  // ---------- Sinks

  @Bean
  @ConditionalOnProperty(prefix = "shared.audit.sinks.kafka", name = "enabled", havingValue = "true")
  @ConditionalOnClass(name = "com.shared.audit.starter.core.dispatch.sinks.KafkaSink")
  public Sink kafkaSink(AuditProperties props) {
    Object k = props.getSinks().getKafka();
    String bootstrap = optionalString(k, "getBootstrapServers", "localhost:9092");
    String topic = optionalString(k, "getTopic", "audit.events.v1");
    String acks = optionalString(k, "getAcks", "all");
    String compression = optionalString(k, "getCompression", "zstd");
    Integer timeoutMs = optionalInt(k, "getTimeoutMs");
    Map<String, Object> conf = new HashMap<>();
    conf.put("acks", acks);
    conf.put("compression.type", compression);
    if (timeoutMs != null) conf.put("delivery.timeout.ms", timeoutMs);

    try {
      Class<?> kClazz = Class.forName("com.shared.audit.starter.core.dispatch.sinks.KafkaSink");

      // Try (String,String,String,String,int)
      try {
        Constructor<?> c = kClazz.getConstructor(String.class, String.class, String.class, String.class, int.class);
        return (Sink) c.newInstance(bootstrap, topic, acks, compression, timeoutMs != null ? timeoutMs : 5000);
      } catch (NoSuchMethodException ignored) {}

      // Try (String,String,Map)
      try {
        Constructor<?> c = kClazz.getConstructor(String.class, String.class, Map.class);
        return (Sink) c.newInstance(bootstrap, topic, conf);
      } catch (NoSuchMethodException ignored) {}

      // Try (String,String)
      Constructor<?> c = kClazz.getConstructor(String.class, String.class);
      return (Sink) c.newInstance(bootstrap, topic);

    } catch (Exception e) {
      throw new IllegalStateException("No compatible KafkaSink constructor found", e);
    }
  }

  @Bean
  @ConditionalOnClass(JdbcTemplate.class)
  @ConditionalOnBean(name = "auditTransactionTemplate")
  @ConditionalOnProperty(prefix = "shared.audit.sinks.db", name = "enabled", havingValue = "true")
  public DatabaseSink databaseSink(JdbcTemplate jdbc,
                                   TransactionTemplate auditTransactionTemplate,
                                   AuditProperties props) {
    Object db = props.getSinks().getDb();
    String schema = optionalString(db, "getSchema", "public");
    String table = optionalString(db, "getTable", "audit_events");
    int batchSize = Optional.ofNullable(optionalInt(db, "getBatchSize")).orElse(200);

    // Try: (JdbcTemplate,TransactionTemplate,String,String,int) -> (JdbcTemplate,TransactionTemplate,String,String)
    // -> (JdbcTemplate,String,String) -> (JdbcTemplate,String)
    try {
      Constructor<DatabaseSink> c = DatabaseSink.class.getConstructor(
          JdbcTemplate.class, TransactionTemplate.class, String.class, String.class, int.class);
      return c.newInstance(jdbc, auditTransactionTemplate, schema, table, batchSize);
    } catch (NoSuchMethodException ignored) {
    } catch (Exception e) {
      throw new IllegalStateException("Failed DatabaseSink(jdbc,tx,schema,table,int)", e);
    }
    try {
      Constructor<DatabaseSink> c = DatabaseSink.class.getConstructor(
          JdbcTemplate.class, TransactionTemplate.class, String.class, String.class);
      return c.newInstance(jdbc, auditTransactionTemplate, schema, table);
    } catch (NoSuchMethodException ignored) {
    } catch (Exception e) {
      throw new IllegalStateException("Failed DatabaseSink(jdbc,tx,schema,table)", e);
    }
    try {
      Constructor<DatabaseSink> c = DatabaseSink.class.getConstructor(JdbcTemplate.class, String.class, String.class);
      return c.newInstance(jdbc, schema, table);
    } catch (NoSuchMethodException ignored) {
    } catch (Exception e) {
      throw new IllegalStateException("Failed DatabaseSink(jdbc,schema,table)", e);
    }
    try {
      Constructor<DatabaseSink> c = DatabaseSink.class.getConstructor(JdbcTemplate.class, String.class);
      return c.newInstance(jdbc, table);
    } catch (Exception e) {
      throw new IllegalStateException("No compatible DatabaseSink constructor found", e);
    }
  }

  @Bean
  @ConditionalOnClass(JdbcTemplate.class)
  @ConditionalOnBean(name = "auditTransactionTemplate")
  @ConditionalOnProperty(prefix = "shared.audit.sinks.outbox", name = "enabled", havingValue = "true")
  public OutboxSink outboxSink(JdbcTemplate jdbc,
                               TransactionTemplate auditTransactionTemplate,
                               AuditProperties props) {
    Object o = props.getSinks().getOutbox();
    String table = optionalString(o, "getTable", "audit_outbox");
    try {
      Constructor<OutboxSink> c = OutboxSink.class.getConstructor(JdbcTemplate.class, TransactionTemplate.class, String.class);
      return c.newInstance(jdbc, auditTransactionTemplate, table);
    } catch (NoSuchMethodException ignored) {
    } catch (Exception e) {
      throw new IllegalStateException("Failed OutboxSink(jdbc,tx,table)", e);
    }
    try {
      Constructor<OutboxSink> c = OutboxSink.class.getConstructor(JdbcTemplate.class, String.class);
      return c.newInstance(jdbc, table);
    } catch (Exception e) {
      throw new IllegalStateException("No compatible OutboxSink constructor found", e);
    }
  }

  @Bean
  @ConditionalOnProperty(prefix = "shared.audit.sinks.otlp", name = "enabled", havingValue = "true")
  @ConditionalOnClass(name = "com.shared.audit.starter.core.dispatch.sinks.OtlpSink")
  public Sink otlpSink(AuditProperties props) {
    Object otlp = optionalGetter(props.getSinks(), "getOtlp", Object.class, null);
    String endpoint = optionalString(otlp, "getEndpoint", "http://otel-collector:4317");
    try {
      Class<?> clazz = Class.forName("com.shared.audit.starter.core.dispatch.sinks.OtlpSink");
      Constructor<?> ctor = clazz.getConstructor(String.class);
      return (Sink) ctor.newInstance(endpoint);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to build OtlpSink", e);
    }
  }

  // ---------- Web filter

  @Bean
  @ConditionalOnClass(name = "jakarta.servlet.Filter")
  @ConditionalOnProperty(prefix = "shared.audit.web", name = "enabled", havingValue = "true", matchIfMissing = true)
  public AuditWebMvcFilter auditWebMvcFilter(AuditService audit, AuditProperties props) {
    AuditWebMvcFilter filter = new AuditWebMvcFilter(audit);

    Object web = props.getWeb();
    Boolean includeHeaders = optionalBoolean(web, "isIncludeHeaders");
    Boolean trackBodies = optionalBoolean(web, "isTrackBodies");
    @SuppressWarnings("unchecked")
    List<String> includePaths = (List<String>) optionalGetter(web, "getIncludePaths", List.class, Collections.emptyList());
    @SuppressWarnings("unchecked")
    List<String> excludePaths = (List<String>) optionalGetter(web, "getExcludePaths", List.class, Collections.emptyList());

    invokeSetterIfPresent(filter, "setIncludeHeaders", boolean.class, includeHeaders);
    invokeSetterIfPresent(filter, "setTrackBodies", boolean.class, trackBodies);
    invokeSetterIfPresent(filter, "setIncludePaths", List.class, includePaths);
    invokeSetterIfPresent(filter, "setExcludePaths", List.class, excludePaths);
    return filter;
  }

  // ---------- reflection helpers

  private static Object callGetter(Object target, String method) {
    if (target == null) return null;
    try {
      Method m = target.getClass().getMethod(method);
      return m.invoke(target);
    } catch (Exception ignored) {
      return null;
    }
  }

  private static <T> T optionalGetter(Object target, String method, Class<T> type, T defaultVal) {
    Object v = callGetter(target, method);
    return (type.isInstance(v)) ? type.cast(v) : defaultVal;
  }

  private static String optionalString(Object target, String getter, String def) {
    Object v = callGetter(target, getter);
    return v != null ? String.valueOf(v) : def;
  }

  private static Integer optionalInt(Object target, String getter) {
    Object v = callGetter(target, getter);
    if (v instanceof Number) return ((Number) v).intValue();
    try { return v != null ? Integer.parseInt(String.valueOf(v)) : null; } catch (Exception ignored) { return null; }
  }

  private static Long optionalLong(Object target, String getter) {
    Object v = callGetter(target, getter);
    if (v instanceof Number) return ((Number) v).longValue();
    try { return v != null ? Long.parseLong(String.valueOf(v)) : null; } catch (Exception ignored) { return null; }
  }

  private static Boolean optionalBoolean(Object target, String getter) {
    Object v = callGetter(target, getter);
    if (v instanceof Boolean) return (Boolean) v;
    return v != null ? Boolean.parseBoolean(String.valueOf(v)) : null;
  }

  private static <T> void invokeSetterIfPresent(Object target, String method, Class<T> type, T value) {
    if (target == null || value == null) return;
    try {
      Method m = target.getClass().getMethod(method, type);
      m.invoke(target, value);
    } catch (NoSuchMethodException ignored) {
      // older artifact – setter not available
    } catch (Exception e) {
      throw new IllegalStateException("Failed invoking " + method + " on " + target.getClass().getSimpleName(), e);
    }
  }
}
