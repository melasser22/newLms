package com.ejada.starter_data.datasource;

import com.ejada.common.tenant.TenantIsolationValidator;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.AbstractDataSource;

/**
 * {@link DataSource} implementation that maintains a dedicated connection pool
 * per tenant. New pools are created lazily via the supplied factory the first
 * time a tenant attempts to obtain a connection. The implementation is thread
 * safe and caches pools until explicitly evicted.
 */
public class TenantAwareDataSource extends AbstractDataSource {

    private final ConcurrentMap<String, DataSource> perTenantPools = new ConcurrentHashMap<>();
    private final Function<String, DataSource> dataSourceFactory;
    private final DataSource defaultDataSource;

    /**
     * Create a new tenant aware data source.
     *
     * @param dataSourceFactory factory used to create pools for individual tenants
     */
    public TenantAwareDataSource(Function<String, DataSource> dataSourceFactory) {
        this(dataSourceFactory, null);
    }

    /**
     * Create a new tenant aware data source with an optional default pool used
     * when no tenant context is available.
     */
    public TenantAwareDataSource(Function<String, DataSource> dataSourceFactory, DataSource defaultDataSource) {
        this.dataSourceFactory = Objects.requireNonNull(dataSourceFactory, "dataSourceFactory");
        this.defaultDataSource = defaultDataSource;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return resolveCurrentDataSource().getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return resolveCurrentDataSource().getConnection(username, password);
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return Logger.getLogger(TenantAwareDataSource.class.getName());
    }

    /**
     * Close and remove the pool associated with the given tenant identifier.
     */
    public void evictTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return;
        }
        DataSource removed = perTenantPools.remove(tenantId);
        if (removed instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception ex) {
                throw new RuntimeException("Failed to close datasource for tenant " + tenantId, ex);
            }
        }
    }

    /**
     * @return unmodifiable view of tenant -> datasource mappings (primarily for testing).
     */
    public Map<String, DataSource> getTenantPools() {
        return java.util.Collections.unmodifiableMap(perTenantPools);
    }

    private DataSource resolveCurrentDataSource() {
        Optional<String> tenantOpt = TenantIsolationValidator.currentTenant();
        if (tenantOpt.isEmpty()) {
            if (defaultDataSource != null) {
                return defaultDataSource;
            }
            throw new IllegalStateException("Tenant context is required to obtain a connection");
        }
        String tenantId = tenantOpt.get();
        UUID tenantUuid;
        try {
            tenantUuid = UUID.fromString(tenantId);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Tenant context is not a valid UUID: " + tenantId, ex);
        }
        TenantIsolationValidator.verifyDatabaseAccess("TenantAwareDataSource", tenantUuid);
        return perTenantPools.computeIfAbsent(tenantId, this::createDataSource);
    }

    private DataSource createDataSource(String tenantId) {
        DataSource dataSource = dataSourceFactory.apply(tenantId);
        if (dataSource == null) {
            throw new IllegalStateException("DataSource factory returned null for tenant " + tenantId);
        }
        return dataSource;
    }
}

