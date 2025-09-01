package com.shared.audit.starter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import com.common.constants.HeaderNames;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "shared.audit")
public class AuditProperties {
  private boolean enabled = true;
  private Web web = new Web();
  private Aop aop = new Aop();
  private Dispatcher dispatcher = new Dispatcher();
  private Masking masking = new Masking();
  private Retention retention = new Retention();
  private Sinks sinks = new Sinks();
  private Tenant tenant = new Tenant();

  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }
  public Web getWeb() { return web; }
  public Aop getAop() { return aop; }
  public Dispatcher getDispatcher() { return dispatcher; }
  public Masking getMasking() { return masking; }
  public Retention getRetention() { return retention; }
  public Sinks getSinks() { return sinks; }
  public Tenant getTenant() { return tenant; }

  public static class Web {
    private boolean enabled = true;
    private boolean includeHeaders = false;
    private boolean trackBodies = false;
    private List<String> includePaths = new ArrayList<>();
    private List<String> excludePaths = new ArrayList<>();
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isIncludeHeaders() { return includeHeaders; }
    public void setIncludeHeaders(boolean includeHeaders) { this.includeHeaders = includeHeaders; }
    public boolean isTrackBodies() { return trackBodies; }
    public void setTrackBodies(boolean trackBodies) { this.trackBodies = trackBodies; }
    public List<String> getIncludePaths() { return includePaths; }
    public List<String> getExcludePaths() { return excludePaths; }
  }

  public static class Aop {
    private boolean enabled = true;
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
  }

  public static class Dispatcher {
    private boolean async = true;
    private int queueCapacity = 50000;
    private int maxDrainBatch = 1000;
    private int maxRetries = 5;
    private long retryBackoffMs = 200;
    public boolean isAsync() { return async; }
    public void setAsync(boolean async) { this.async = async; }
    public int getQueueCapacity() { return queueCapacity; }
    public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }
    public int getMaxDrainBatch() { return maxDrainBatch; }
    public void setMaxDrainBatch(int maxDrainBatch) { this.maxDrainBatch = maxDrainBatch; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public long getRetryBackoffMs() { return retryBackoffMs; }
    public void setRetryBackoffMs(long retryBackoffMs) { this.retryBackoffMs = retryBackoffMs; }
  }

  public static class Masking {
    private boolean enabled = true;
    private List<String> jsonPaths = new ArrayList<>();
    private List<String> fieldsByKey = new ArrayList<>();
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public List<String> getJsonPaths() { return jsonPaths; }
    public List<String> getFieldsByKey() { return fieldsByKey; }
  }

  public static class Retention {
    private boolean enabled = false;
    private int days = 365;
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getDays() { return days; }
    public void setDays(int days) { this.days = days; }
  }

  public static class Sinks {
    @NestedConfigurationProperty public Kafka kafka = new Kafka();
    @NestedConfigurationProperty public Db db = new Db();
    @NestedConfigurationProperty public Outbox outbox = new Outbox();
    @NestedConfigurationProperty public Otlp otlp = new Otlp();
    public Kafka getKafka() { return kafka; }
    public Db getDb() { return db; }
    public Outbox getOutbox() { return outbox; }
    public Otlp getOtlp() { return otlp; }

    public static class Kafka {
      private boolean enabled = false;
      private String topic = "audit.events.v1";
      private String bootstrapServers;
      private String acks = "all";
      private String compression = "zstd";
      private int timeoutMs = 5000;
      public boolean isEnabled() { return enabled; }
      public void setEnabled(boolean enabled) { this.enabled = enabled; }
      public String getTopic() { return topic; }
      public void setTopic(String topic) { this.topic = topic; }
      public String getBootstrapServers() { return bootstrapServers; }
      public void setBootstrapServers(String bootstrapServers) { this.bootstrapServers = bootstrapServers; }
      public String getAcks() { return acks; }
      public void setAcks(String acks) { this.acks = acks; }
      public String getCompression() { return compression; }
      public void setCompression(String compression) { this.compression = compression; }
      public int getTimeoutMs() { return timeoutMs; }
      public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    }

    public static class Db {
      private boolean enabled = false;
      private String table = "audit_events";
      private String schema = "public";
      private int batchSize = 200;
      public boolean isEnabled() { return enabled; }
      public void setEnabled(boolean enabled) { this.enabled = enabled; }
      public String getTable() { return table; }
      public void setTable(String table) { this.table = table; }
      public String getSchema() { return schema; }
      public void setSchema(String schema) { this.schema = schema; }
      public int getBatchSize() { return batchSize; }
      public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    }

    public static class Outbox {
      private boolean enabled = false;
      private String table = "audit_outbox";
      public boolean isEnabled() { return enabled; }
      public void setEnabled(boolean enabled) { this.enabled = enabled; }
      public String getTable() { return table; }
      public void setTable(String table) { this.table = table; }
    }

    public static class Otlp {
      private boolean enabled = false;
      private String endpoint = "http://otel-collector:4317";
      public boolean isEnabled() { return enabled; }
      public void setEnabled(boolean enabled) { this.enabled = enabled; }
      public String getEndpoint() { return endpoint; }
      public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    }
  }

  public static class Tenant {
    private String header = HeaderNames.X_TENANT_ID;
    private boolean required = true;
    public String getHeader() { return header; }
    public void setHeader(String header) { this.header = header; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
  }
}
