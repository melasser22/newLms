package com.lms.tenant.config;

import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Wraps a DataSource so every borrowed Connection sets app.current_tenant from TenantContext. */
public class TenantAwareDataSource extends DelegatingDataSource {
  public TenantAwareDataSource(DataSource targetDataSource) { super(targetDataSource); }

  @Override public Connection getConnection() throws SQLException { return init(super.getConnection()); }
  @Override public Connection getConnection(String username, String password) throws SQLException { return init(super.getConnection(username, password)); }

  private Connection init(Connection c) throws SQLException {
    String tid = TenantContext.get();
    if (tid != null) {
      try (Statement st = c.createStatement()) {
        st.execute("select set_config('app.current_tenant', '" + tid + "', true)");
      }
    }
    return c;
  }
}
