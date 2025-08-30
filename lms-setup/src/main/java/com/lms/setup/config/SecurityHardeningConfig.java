package com.lms.setup.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityHardeningConfig {

    @Value("${setup.security.public-access:false}")
    private boolean publicAccess;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> {
                authz.requestMatchers("/actuator/**").permitAll();
                authz.requestMatchers("/v3/api-docs/**").permitAll();
                authz.requestMatchers("/swagger-ui/**").permitAll();
                authz.requestMatchers("/swagger-ui.html").permitAll();
                authz.requestMatchers("/health/**").permitAll();
                if (publicAccess) {
                    authz.anyRequest().permitAll();
                } else {
                    authz.requestMatchers("/setup/countries/**").hasAnyRole("ADMIN", "USER");
                    authz.requestMatchers("/setup/cities/**").hasAnyRole("ADMIN", "USER");
                    authz.requestMatchers("/setup/lookups/**").hasAnyRole("ADMIN", "USER");
                    authz.requestMatchers("/setup/resources/**").hasAnyRole("ADMIN", "USER");
                    authz.requestMatchers("/setup/system-parameters/**").hasRole("ADMIN");
                    authz.anyRequest().authenticated();
                }
            })
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                    .preload(true)
                )
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .permissionsPolicy(permissions -> permissions
                    .policy("geolocation=(), microphone=(), camera=(), payment=(), usb=(), magnetometer=(), gyroscope=()")
                )
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("https://*.lms.com", "http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "X-Correlation-ID", "X-Tenant-Id"));
        configuration.setExposedHeaders(Arrays.asList("X-Correlation-ID", "X-Tenant-Id"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
