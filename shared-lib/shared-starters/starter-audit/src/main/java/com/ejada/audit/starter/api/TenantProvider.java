package com.ejada.audit.starter.api;

/** SPI to provide current tenant id from the host app */
@FunctionalInterface
public interface TenantProvider {
	String getTenantId(); // return null if unknown
}
