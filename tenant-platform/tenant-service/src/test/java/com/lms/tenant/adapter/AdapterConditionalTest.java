package com.lms.tenant.adapter;

import com.lms.tenant.core.TenantDirectoryPort;
import com.lms.tenant.core.TenantSettingsPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class AdapterConditionalTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(NamedParameterJdbcTemplate.class, () -> mock(NamedParameterJdbcTemplate.class))
            .withUserConfiguration(
                    JdbcTenantDirectoryAdapter.class,
                    JdbcTenantSettingsAdapter.class,
                    SharedTenantDirectoryAdapter.class,
                    SharedTenantSettingsAdapter.class
            );

    @Test
    void jdbcBeansLoadWhenNoSharedClasses() {
        contextRunner.withClassLoader(new FilteredClassLoader("com.shared"))
                .run(ctx -> {
                    TenantDirectoryPort dir = ctx.getBean(TenantDirectoryPort.class);
                    TenantSettingsPort set = ctx.getBean(TenantSettingsPort.class);
                    System.out.println("Active TenantDirectoryPort bean: " + dir.getClass().getSimpleName());
                    System.out.println("Active TenantSettingsPort bean: " + set.getClass().getSimpleName());
                    assertThat(dir).isInstanceOf(JdbcTenantDirectoryAdapter.class);
                    assertThat(set).isInstanceOf(JdbcTenantSettingsAdapter.class);
                });
    }

    @Test
    void sharedBeansLoadWhenSharedClassesPresent() {
        contextRunner.run(ctx -> {
            TenantDirectoryPort dir = ctx.getBean(TenantDirectoryPort.class);
            TenantSettingsPort set = ctx.getBean(TenantSettingsPort.class);
            System.out.println("Active TenantDirectoryPort bean: " + dir.getClass().getSimpleName());
            System.out.println("Active TenantSettingsPort bean: " + set.getClass().getSimpleName());
            assertThat(dir).isInstanceOf(SharedTenantDirectoryAdapter.class);
            assertThat(set).isInstanceOf(SharedTenantSettingsAdapter.class);
        });
    }
}
