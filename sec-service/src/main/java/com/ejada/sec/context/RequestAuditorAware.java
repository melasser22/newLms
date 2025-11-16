package com.ejada.sec.context;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

/**
 * Spring Data {@link AuditorAware} implementation that resolves the current actor
 * from the {@link RequestAuditContextProvider}. The provider already knows how
 * to extract usernames/emails from the authenticated JWT token, so we simply
 * delegate to it for populating {@code created_by} / {@code updated_by} columns.
 */
@Component("requestAuditorAware")
public class RequestAuditorAware implements AuditorAware<String> {

  private final RequestAuditContextProvider requestAuditContextProvider;

  public RequestAuditorAware(RequestAuditContextProvider requestAuditContextProvider) {
    this.requestAuditContextProvider = requestAuditContextProvider;
  }

  @Override
  public Optional<String> getCurrentAuditor() {
    return requestAuditContextProvider.resolveUpdatedBy();
  }
}
