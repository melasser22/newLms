package com.ejada.sec.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ejada.common.events.tenant.TenantProvisioningEvent;
import com.ejada.common.events.tenant.TenantProvisioningEvent.TenantAdminInfo;
import com.ejada.common.events.tenant.TenantProvisioningEvent.TenantCustomerInfo;
import com.ejada.sec.domain.Role;
import com.ejada.sec.domain.User;
import com.ejada.sec.domain.UserRole;
import com.ejada.sec.domain.UserRoleId;
import com.ejada.sec.repository.RoleRepository;
import com.ejada.sec.repository.UserRepository;
import com.ejada.sec.repository.UserRoleRepository;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

class TenantAdminProvisioningServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private TenantAdminProvisioningService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new TenantAdminProvisioningService(userRepository, roleRepository, userRoleRepository, passwordEncoder);
    }

    @Test
    void createsAdminUserWhenNotPresent() {
        TenantProvisioningEvent event = provisioningEvent();
        UUID tenantId = event.tenantId();

        when(userRepository.findByTenantIdAndUsername(tenantId, "m.alqahtani")).thenReturn(Optional.empty());
        when(userRepository.existsByTenantIdAndEmail(tenantId, "m.alqahtani@alnoursolutions.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");

        Role role = Role.builder().id(5L).tenantId(tenantId).code("TENANT_ADMIN").name("Tenant Administrator").build();
        when(roleRepository.findByTenantIdAndCode(tenantId, "TENANT_ADMIN")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenReturn(role);

        User saved = new User();
        saved.setId(42L);
        saved.setTenantId(tenantId);
        saved.setUsername("m.alqahtani");
        saved.setEmail("m.alqahtani@alnoursolutions.com");
        saved.setPasswordHash("hashed");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        when(userRoleRepository.existsById(new UserRoleId(42L, 5L))).thenReturn(false);

        service.provisionTenantAdmin(event);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getTenantId()).isEqualTo(tenantId);
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("m.alqahtani");
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("m.alqahtani@alnoursolutions.com");
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("hashed");

        ArgumentCaptor<UserRole> linkCaptor = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(linkCaptor.capture());
        assertThat(linkCaptor.getValue().getId()).isEqualTo(new UserRoleId(42L, 5L));
    }

    @Test
    void skipsWhenAdminInfoMissing() {
        TenantProvisioningEvent event = new TenantProvisioningEvent(5178L, UUID.randomUUID(), "5178", "9054", customerInfo(), null);

        service.provisionTenantAdmin(event);

        verifyNoInteractions(userRepository, roleRepository, userRoleRepository, passwordEncoder);
    }

    @Test
    void updatesEmailForExistingAdmin() {
        TenantProvisioningEvent event = provisioningEvent();
        UUID tenantId = event.tenantId();

        User existing = new User();
        existing.setId(11L);
        existing.setTenantId(tenantId);
        existing.setUsername("m.alqahtani");
        existing.setEmail("old@example.com");

        when(userRepository.findByTenantIdAndUsername(tenantId, "m.alqahtani")).thenReturn(Optional.of(existing));
        when(userRepository.findByTenantIdAndEmail(tenantId, "m.alqahtani@alnoursolutions.com"))
                .thenReturn(Optional.of(existing));

        Role role = Role.builder().id(5L).tenantId(tenantId).code("TENANT_ADMIN").name("Tenant Administrator").build();
        when(roleRepository.findByTenantIdAndCode(tenantId, "TENANT_ADMIN")).thenReturn(Optional.of(role));
        when(userRoleRepository.existsById(new UserRoleId(11L, 5L))).thenReturn(true);

        service.provisionTenantAdmin(event);

        verify(userRepository).save(existing);
        assertThat(existing.getEmail()).isEqualTo("m.alqahtani@alnoursolutions.com");
        verify(userRoleRepository, never()).save(any(UserRole.class));
    }

    private TenantProvisioningEvent provisioningEvent() {
        UUID tenantId = UUID.nameUUIDFromBytes("tenant:9054".getBytes(StandardCharsets.UTF_8));
        return new TenantProvisioningEvent(5178L, tenantId, "5178", "9054", customerInfo(),
                new TenantAdminInfo("m.alqahtani", "m.alqahtani@alnoursolutions.com", "+966501234567", "AR"));
    }

    private TenantCustomerInfo customerInfo() {
        return new TenantCustomerInfo(
                "Al-Nour Solutions Co.",
                "شركة النور للحلول",
                "BUSINESS",
                "1019876543",
                "SA",
                "JED",
                "Prince Sultan Rd, Al Zahra, Jeddah",
                "طريق الأمير سلطان، الزهراء، جدة",
                "accounts@alnoursolutions.com",
                "+966544556677");
    }
}
