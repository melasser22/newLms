package com.lms.tenant.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import com.common.context.TenantContext;

/**
 * Wraps {@link DataSource} connections to apply the current tenant using a PostgreSQL GUC variable.
 */
public class TenantConnectionInterceptor extends DelegatingDataSource {

    public TenantConnectionInterceptor(DataSource targetDataSource) {
        super(targetDataSource);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = super.getConnection();
        applyTenant(connection);
        return connection;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = super.getConnection(username, password);
        applyTenant(connection);
        return connection;
    }

    private void applyTenant(Connection connection) throws SQLException {
        if (connection == null) {
            return;
        }
        var tenantId = TenantContext.get();
        if (tenantId.isPresent()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET LOCAL app.current_tenant = '" + tenantId.get() + "'");
            }
        }
    }
}
