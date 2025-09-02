package com.ejada.audit.starter.core.dispatch;

import com.ejada.audit.starter.api.AuditEvent;
import com.ejada.audit.starter.core.dispatch.sinks.Sink;

import java.util.ArrayList;
import java.util.List;

public class AuditDispatcher {
  private final List<Sink> sinks = new ArrayList<>();

  public AuditDispatcher(List<Sink> sinks) {
    if (sinks != null) this.sinks.addAll(sinks);
  }

  public void dispatch(AuditEvent event) {
    for (Sink s : sinks) {
      try { s.send(event); }
      catch (Exception e) { /* log & continue */ }
    }
  }
}
