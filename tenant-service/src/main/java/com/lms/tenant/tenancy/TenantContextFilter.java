package com.lms.tenant.tenancy;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

import javax.sql.DataSource;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TenantContextFilter extends OncePerRequestFilter {

    private final DataSource dataSource;
    private final TenantResolver tenantResolver = new TenantResolver();

    public TenantContextFilter(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var tenantOpt = tenantResolver.resolveTenant(request);
        tenantOpt.ifPresent(this::setTenant);
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void setTenant(UUID tenantId) {
        TenantContext.setTenantId(tenantId);
        Connection con = DataSourceUtils.getConnection(dataSource);
        try (Statement st = con.createStatement()) {
            st.execute("select set_config('app.current_tenant', '" + tenantId + "', true)");
        } catch (Exception e) {
            // ignore for now
        } finally {
            DataSourceUtils.releaseConnection(con, dataSource);
        }
    }
}
