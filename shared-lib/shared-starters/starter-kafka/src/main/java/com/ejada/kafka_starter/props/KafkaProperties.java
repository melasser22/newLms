package com.ejada.kafka_starter.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

@ConfigurationProperties(prefix = "shared.kafka")
public class KafkaProperties {
    /** Bootstrap servers, comma-separated */
    private String bootstrapServers = "localhost:9092";

    /** app env: dev|stg|prod used in topic naming */
    private String env = "dev";

    /** logical domain for topics, e.g. "points", "voucher" */
    private String domain = "shared";

    /** default clientId prefix */
    private String clientId = "shared";

    /** producer tx prefix for EOS */
    private String txIdPrefix = "shared-tx-";

    /** enable exactly-once producer (idempotence=true, transactions) */
    private boolean exactlyOnce = true;

    // ---------- Producer tuning (اختياري) ----------
    /** linger for batching */
    private Duration linger = Duration.ofMillis(5);
    /** batch size (bytes) */
    private int batchSize = 32_768;
    /** buffer memory (bytes) */
    private long bufferMemory = 33_554_432;
    /** compression: none|lz4|snappy|gzip|zstd */
    private String compression = "lz4";
    /** max request size (bytes) */
    private int maxRequestSize = 1_048_576;

    // ---------- Consumer ----------
    /** Consumer concurrency */
    private int concurrency = 3;
    /** default group id (if not set on @KafkaListener) */
    private String groupId = "shared-consumer";
    /** earliest|latest|none */
    private String autoOffsetReset = "earliest";
    /** max poll records per fetch */
    private int maxPollRecords = 500;
    /** session timeout */
    private Duration sessionTimeout = Duration.ofSeconds(45);
    /** heartbeat interval */
    private Duration heartbeatInterval = Duration.ofSeconds(15);

    // ---------- Retry/DLT ----------
    /** retry attempts before sending to DLT */
    private int maxAttempts = 5;
    /** backoff between attempts */
    private Duration backoff = Duration.ofMillis(500);

    // ---------- Topics ----------
    /** create topics automatically (if ACLs allow) */
    private boolean autoCreateTopics = false;
    /** optional map of topic -> "partitions,replicas" (e.g. "dev.points.events":"12,3") */
    private Map<String, String> topics;

    // ---------- Idempotency / headers ----------
    /** Default TTL (seconds) for idempotency store */
    private long idempotencyTtlSeconds = 24 * 3600;
    /** Default schema version to attach in headers */
    private String schemaVersion = "1";

    // ----- getters & setters -----
    public String getBootstrapServers() { return bootstrapServers; }
    public void setBootstrapServers(String bootstrapServers) { this.bootstrapServers = bootstrapServers; }

    public String getEnv() { return env; }
    public void setEnv(String env) { this.env = env; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getTxIdPrefix() { return txIdPrefix; }
    public void setTxIdPrefix(String txIdPrefix) { this.txIdPrefix = txIdPrefix; }

    public boolean isExactlyOnce() { return exactlyOnce; }
    public void setExactlyOnce(boolean exactlyOnce) { this.exactlyOnce = exactlyOnce; }

    public Duration getLinger() { return linger; }
    public void setLinger(Duration linger) { this.linger = linger; }

    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }

    public long getBufferMemory() { return bufferMemory; }
    public void setBufferMemory(long bufferMemory) { this.bufferMemory = bufferMemory; }

    public String getCompression() { return compression; }
    public void setCompression(String compression) { this.compression = compression; }

    public int getMaxRequestSize() { return maxRequestSize; }
    public void setMaxRequestSize(int maxRequestSize) { this.maxRequestSize = maxRequestSize; }

    public int getConcurrency() { return concurrency; }
    public void setConcurrency(int concurrency) { this.concurrency = concurrency; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getAutoOffsetReset() { return autoOffsetReset; }
    public void setAutoOffsetReset(String autoOffsetReset) { this.autoOffsetReset = autoOffsetReset; }

    public int getMaxPollRecords() { return maxPollRecords; }
    public void setMaxPollRecords(int maxPollRecords) { this.maxPollRecords = maxPollRecords; }

    public Duration getSessionTimeout() { return sessionTimeout; }
    public void setSessionTimeout(Duration sessionTimeout) { this.sessionTimeout = sessionTimeout; }

    public Duration getHeartbeatInterval() { return heartbeatInterval; }
    public void setHeartbeatInterval(Duration heartbeatInterval) { this.heartbeatInterval = heartbeatInterval; }

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

    public Duration getBackoff() { return backoff; }
    public void setBackoff(Duration backoff) { this.backoff = backoff; }

    public boolean isAutoCreateTopics() { return autoCreateTopics; }
    public void setAutoCreateTopics(boolean autoCreateTopics) { this.autoCreateTopics = autoCreateTopics; }

    public Map<String, String> getTopics() { return topics; }
    public void setTopics(Map<String, String> topics) { this.topics = topics; }

    public long getIdempotencyTtlSeconds() { return idempotencyTtlSeconds; }
    public void setIdempotencyTtlSeconds(long idempotencyTtlSeconds) { this.idempotencyTtlSeconds = idempotencyTtlSeconds; }

    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }
}
