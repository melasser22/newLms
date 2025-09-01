package com.common.logging;

import com.common.constants.HeaderNames;

import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;

/**
 * Centralized log markers for structured logging.
 * These markers enrich logs with key/value fields
 * (automatically indexed in ELK/Grafana).
 */
public final class LogMarkers {

    private LogMarkers() {
        // utility class
    }

    /** Correlation markers (always attach correlationId + tenantId) */
    public static LogstashMarker correlation(String correlationId, String tenantId) {
        LogstashMarker marker = Markers.append("correlationId", correlationId);
        if (tenantId != null && !tenantId.isBlank()) {
            marker = marker.and(Markers.append(HeaderNames.X_TENANT_ID, tenantId));
        }
        return marker;
    }

    /** Security events (auth, access control, etc.) */
    public static LogstashMarker security(String action, String user) {
        return Markers.append("eventType", "SECURITY")
                .and(Markers.append("action", action))
                .and(Markers.append("user", user));
    }

    /** Business events (transactions, domain-specific actions) */
    public static LogstashMarker business(String event, String entityId) {
        return Markers.append("eventType", "BUSINESS")
                .and(Markers.append("event", event))
                .and(Markers.append("entityId", entityId));
    }

    /** Error markers with errorCode */
    public static LogstashMarker error(String errorCode) {
        return Markers.append("eventType", "ERROR")
                .and(Markers.append("errorCode", errorCode));
    }

    /** Performance metrics (timing, counts, etc.) */
    public static LogstashMarker metric(String name, long value) {
        return Markers.append("metric", name)
                .and(Markers.append("value", value));
    }

    /** correlation marker */
    public static LogstashMarker correlation(String correlationId) {
        return Markers.append("correlationId", correlationId);
    }
}
