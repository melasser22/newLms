package com.ejada.starter_core.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.constants.RequestAttributeNames;
import com.ejada.common.context.ContextManager;
import com.ejada.common.context.TenantMdcUtil;
import com.ejada.starter_core.config.CoreAutoConfiguration;
import com.ejada.starter_core.tenant.TenantResolution;
import com.ejada.starter_core.tenant.TenantResolver;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TenantContextContributorTest {

    @AfterEach
    void tearDown() {
        ContextManager.Tenant.clear();
        TenantMdcUtil.clear();
        MDC.clear();
    }

    @Test
    void populatesRequestAttributesAndMdc() throws ServletException, IOException {
        CoreAutoConfiguration.CoreProps props = new CoreAutoConfiguration.CoreProps();
        CoreAutoConfiguration.CoreProps.Tenant tenantProps = props.getTenant();
        AtomicBoolean invoked = new AtomicBoolean();
        TenantResolver resolver = request -> {
            invoked.set(true);
            return TenantResolution.present("abc-tenant", null);
        };
        TenantContextContributor contributor = new TenantContextContributor(resolver, tenantProps);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        RequestContextContributor.ContextScope scope = contributor.contribute(request, response);

        assertThat(invoked).isTrue();
        assertThat(scope).isNotNull();
        assertThat(request.getAttribute(RequestAttributeNames.TENANT_ID)).isEqualTo("abc-tenant");
        assertThat(request.getAttribute(tenantProps.getHeaderName())).isEqualTo("abc-tenant");
        assertThat(request.getAttribute(HeaderNames.X_TENANT_ID)).isEqualTo("abc-tenant");
        assertThat(response.getHeader(tenantProps.getHeaderName())).isEqualTo("abc-tenant");
        assertThat(ContextManager.Tenant.get()).isEqualTo("abc-tenant");
        assertThat(MDC.get(tenantProps.getMdcKey())).isEqualTo("abc-tenant");
        assertThat(TenantMdcUtil.getTenantId()).isEqualTo("abc-tenant");

        scope.close();

        assertThat(ContextManager.Tenant.get()).isNull();
        assertThat(MDC.get(tenantProps.getMdcKey())).isNull();
        assertThat(TenantMdcUtil.getTenantId()).isNull();
    }
}
