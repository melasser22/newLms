package com.ejada.audit.starter.core.enrich;

import com.ejada.audit.starter.api.AuditEvent;
import com.ejada.audit.starter.api.context.Actor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

public class SecurityEnricher implements Enricher {
  @Override public void enrich(AuditEvent.Builder b) {
    try {
      Authentication a = SecurityContextHolder.getContext().getAuthentication();
      if (a != null && a.isAuthenticated()) {
        List<String> roles = a.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        String username = a.getName();
        b.actor(new Actor(username, username, roles, "JWT"));
      }
    } catch (Throwable ignore) { }
  }
}
