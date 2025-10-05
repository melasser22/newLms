package com.ejada.sec.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventSeverity {
    INFO(0, "Informational"),
    LOW(25, "Low priority"),
    MEDIUM(50, "Medium priority - monitor"),
    HIGH(75, "High priority - review required"),
    CRITICAL(100, "Critical - immediate action required");

    private final int score;
    private final String description;

    public static EventSeverity fromScore(int score) {
        if (score >= 100) return CRITICAL;
        if (score >= 75) return HIGH;
        if (score >= 50) return MEDIUM;
        if (score >= 25) return LOW;
        return INFO;
    }
}
