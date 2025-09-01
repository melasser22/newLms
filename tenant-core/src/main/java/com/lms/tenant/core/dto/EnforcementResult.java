package com.lms.tenant.core.dto;

import java.util.UUID;

/** Result of enforcing a feature policy. */
public final class EnforcementResult {

    public enum Type {
        ALLOWED_UNLIMITED,
        ALLOWED_WITHIN_LIMIT,
        ALLOWED_WITH_OVERAGE
        }

    private final String featureKey;
    private final Type type;
    private final Long limit;
    private final long usedBefore;
    private final long requestedDelta;
    private final long overageRecorded;
    private final UUID overageId;

    private EnforcementResult(String featureKey, Type type, Long limit, long usedBefore, long requestedDelta,
                              long overageRecorded, UUID overageId) {
        this.featureKey = featureKey;
        this.type = type;
        this.limit = limit;
        this.usedBefore = usedBefore;
        this.requestedDelta = requestedDelta;
        this.overageRecorded = overageRecorded;
        this.overageId = overageId;
    }

    public String featureKey() {return featureKey;}
    public Type type() {return type;}
    public Long limit() {return limit;}
    public long usedBefore() {return usedBefore;}
    public long requestedDelta() {return requestedDelta;}
    public long overageRecorded() {return overageRecorded;}
    public UUID overageId() {return overageId;}

    public static EnforcementResult allowedUnlimited(String featureKey) {
        return new EnforcementResult(featureKey, Type.ALLOWED_UNLIMITED, null, 0, 0, 0, null);
    }

    public static EnforcementResult allowedWithinLimit(String featureKey, long limit, long usedBefore, long requestedDelta) {
        return new EnforcementResult(featureKey, Type.ALLOWED_WITHIN_LIMIT, limit, usedBefore, requestedDelta, 0, null);
    }

    public static EnforcementResult allowedWithOverage(String featureKey, long limit, long usedBefore,
                                                       long requestedDelta, long overageRecorded, UUID overageId) {
        return new EnforcementResult(featureKey, Type.ALLOWED_WITH_OVERAGE, limit, usedBefore, requestedDelta,
                overageRecorded, overageId);
    }
}
