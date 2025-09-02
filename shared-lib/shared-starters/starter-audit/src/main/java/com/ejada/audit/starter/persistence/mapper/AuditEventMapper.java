package com.ejada.audit.starter.persistence.mapper;

import com.ejada.audit.starter.api.AuditEvent;
import com.ejada.audit.starter.persistence.entity.AuditEventEntity;
import com.ejada.audit.starter.util.JsonUtils;
import com.ejada.common.exception.JsonSerializationException;

public final class AuditEventMapper {
  private AuditEventMapper() { }
  public static AuditEventEntity toEntity(AuditEvent e) {
    AuditEventEntity en = new AuditEventEntity();
    try {
      var idF = AuditEventEntity.class.getDeclaredField("id"); idF.setAccessible(true); idF.set(en, e.getEventId());
      var tsF = AuditEventEntity.class.getDeclaredField("tsUtc"); tsF.setAccessible(true); tsF.set(en, e.getTimestamp());
      var tF  = AuditEventEntity.class.getDeclaredField("xTenantId"); tF.setAccessible(true); tF.set(en, e.getTenantId());
      var aF  = AuditEventEntity.class.getDeclaredField("actorId"); aF.setAccessible(true); aF.set(en, e.getActor()==null?null:e.getActor().id());
      var auF = AuditEventEntity.class.getDeclaredField("actorUsername"); auF.setAccessible(true); auF.set(en, e.getActor()==null?null:e.getActor().username());
      var acF = AuditEventEntity.class.getDeclaredField("action"); acF.setAccessible(true); acF.set(en, e.getAction().name());
      var etF = AuditEventEntity.class.getDeclaredField("entityType"); etF.setAccessible(true); etF.set(en, e.getEntityType());
      var eiF = AuditEventEntity.class.getDeclaredField("entityId"); eiF.setAccessible(true); eiF.set(en, e.getEntityId());
      var ocF = AuditEventEntity.class.getDeclaredField("outcome"); ocF.setAccessible(true); ocF.set(en, e.getOutcome().name());
      var msF = AuditEventEntity.class.getDeclaredField("message"); msF.setAccessible(true); msF.set(en, e.getMessage());
    } catch (Exception ignore) {
      // ignore reflection exceptions; fields may not be accessible or present
    }
    // Serialize payload separately; handle checked exception
    try {
      String payload = JsonUtils.toJson(e);
      var plF = AuditEventEntity.class.getDeclaredField("payload");
      plF.setAccessible(true);
      plF.set(en, payload);
    } catch (JsonSerializationException jsonEx) {
      // If serialization fails, leave payload as null to avoid crashing
    } catch (Exception ignore) {
      // ignore other reflection issues
    }
    return en;
  }
}
