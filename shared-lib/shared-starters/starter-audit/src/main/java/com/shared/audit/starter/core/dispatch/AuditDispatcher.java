package com.shared.audit.starter.core.dispatch;

import com.shared.audit.starter.api.AuditEvent;
import com.shared.audit.starter.core.dispatch.sinks.Sink;

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
