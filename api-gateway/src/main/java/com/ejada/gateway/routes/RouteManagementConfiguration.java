package com.ejada.gateway.routes;

import com.ejada.gateway.routes.repository.RouteDefinitionR2dbcRepository;
import com.ejada.gateway.security.mtls.PartnerCertificateRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@EnableR2dbcRepositories(basePackageClasses = {
    RouteDefinitionR2dbcRepository.class,
    PartnerCertificateRepository.class
})
public class RouteManagementConfiguration {
}
