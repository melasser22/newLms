package com.lms.setup.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Locale;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.lms.setup.repository")
public class DatabaseConfig {

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public HikariConfig hikariConfig() {
        return new HikariConfig();
    }

    @Bean
    @Primary
    public DataSource dataSource(
            DataSourceProperties properties, HikariConfig hikariConfig, Environment env) {
        String jdbcUrl = properties.getUrl();
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            // Allow overriding via DB_URL env var as documented in README
            jdbcUrl = env.getProperty(
                    "DB_URL",
                    "jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH");
            hikariConfig.setUsername(env.getProperty("DB_USERNAME", "sa"));
            hikariConfig.setPassword(env.getProperty("DB_PASSWORD", ""));
            String driver = jdbcUrl.startsWith("jdbc:h2")
                    ? "org.h2.Driver"
                    : (properties.getDriverClassName() != null
                            ? properties.getDriverClassName()
                            : "org.postgresql.Driver");
            hikariConfig.setDriverClassName(driver);
        } else {
            hikariConfig.setUsername(properties.getUsername());
            hikariConfig.setPassword(properties.getPassword());
            hikariConfig.setDriverClassName(properties.getDriverClassName());
        }
        hikariConfig.setJdbcUrl(jdbcUrl);

        // Enhanced connection pooling settings
        hikariConfig.setMaximumPoolSize(20);
        hikariConfig.setMinimumIdle(5);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);
        hikariConfig.setLeakDetectionThreshold(60000);

        // Performance optimizations
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setValidationTimeout(5000);
        hikariConfig.setInitializationFailTimeout(-1);

        // Monitoring and metrics
        hikariConfig.setPoolName("LMS-HikariCP");
        hikariConfig.setRegisterMbeans(true);

        return new HikariDataSource(hikariConfig);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            DataSource dataSource, Environment env, DataSourceProperties dataSourceProperties) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.lms.setup.model");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(false);
        vendorAdapter.setShowSql(false);
        String dialect = "org.hibernate.dialect.PostgreSQLDialect";
        if (dataSourceProperties.getDriverClassName() != null
                && dataSourceProperties
                        .getDriverClassName()
                        .toLowerCase(Locale.ROOT)
                        .contains("h2")) {
            dialect = "org.hibernate.dialect.H2Dialect";
        }
        vendorAdapter.setDatabasePlatform(dialect);
        em.setJpaVendorAdapter(vendorAdapter);

        Properties properties = new Properties();
        
        // Core Hibernate settings
        properties.setProperty("hibernate.hbm2ddl.auto", "validate");
        properties.setProperty("hibernate.dialect", dialect);
        properties.setProperty("hibernate.show_sql", "false");
        properties.setProperty("hibernate.format_sql", "true");
        properties.setProperty("hibernate.use_sql_comments", "false");
        
        // Performance optimizations
        properties.setProperty("hibernate.jdbc.batch_size", "20");
        properties.setProperty("hibernate.order_inserts", "true");
        properties.setProperty("hibernate.order_updates", "true");
        properties.setProperty("hibernate.jdbc.batch_versioned_data", "true");
        properties.setProperty("hibernate.connection.provider_disables_autocommit", "true");
        
        // Connection pooling
        // Use the official Hibernate module for HikariCP
        properties.setProperty(
                "hibernate.connection.provider_class",
                "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");
        properties.setProperty("hibernate.hikari.connectionTimeout", "30000");
        properties.setProperty("hibernate.hikari.maximumPoolSize", "20");
        properties.setProperty("hibernate.hikari.minimumIdle", "5");
        
        // Second-level cache
        if (!env.acceptsProfiles(Profiles.of("test"))) {
            properties.setProperty("hibernate.cache.use_second_level_cache", "true");
            properties.setProperty("hibernate.cache.use_query_cache", "true");
            properties.setProperty(
                    "hibernate.cache.region.factory_class",
                    "org.hibernate.cache.jcache.JCacheRegionFactory");
        } else {
            properties.setProperty("hibernate.cache.use_second_level_cache", "false");
            properties.setProperty("hibernate.cache.use_query_cache", "false");
        }
        
        // Statistics and monitoring
        properties.setProperty("hibernate.generate_statistics", "false");
        properties.setProperty("hibernate.session.events.log", "false");
        
        em.setJpaProperties(properties);

        return em;
    }

    @Bean
    public PlatformTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
        transactionManager.setDefaultTimeout(30); // 30 seconds default timeout
        return transactionManager;
    }
}