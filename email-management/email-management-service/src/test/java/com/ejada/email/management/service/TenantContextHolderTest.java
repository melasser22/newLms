package com.ejada.email.management.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantContextHolderTest {

  @AfterEach
  void tearDown() {
    TenantContextHolder.clear();
  }

  @Test
  void shouldStoreAndRetrieveTenantId() {
    TenantContextHolder.setTenantId("tenant-123");

    assertThat(TenantContextHolder.getTenantId()).contains("tenant-123");
  }

  @Test
  void shouldClearTenantId() {
    TenantContextHolder.setTenantId("tenant-abc");

    TenantContextHolder.clear();

    assertThat(TenantContextHolder.getTenantId()).isEmpty();
  }
}
