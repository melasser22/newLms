package com.ejada.sec.repository;

import com.ejada.common.context.ContextManager;
import com.ejada.common.exception.ValidationException;
import com.ejada.sec.domain.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class UserRepositoryTenantIsolationTest {

    @Autowired
    private UserRepository userRepository;

    private UUID tenantOne;
    private UUID tenantTwo;
    private Long tenantOneUserId;
    private Long tenantTwoUserId;

    @BeforeEach
    void setUp() {
        tenantOne = UUID.randomUUID();
        tenantTwo = UUID.randomUUID();

        tenantOneUserId = userRepository.save(buildUser(tenantOne, "tenant1@example.com", "tenant1"))
                .getId();
        tenantTwoUserId = userRepository.save(buildUser(tenantTwo, "tenant2@example.com", "tenant2"))
                .getId();
    }

    @AfterEach
    void tearDown() {
        ContextManager.Tenant.clear();
    }

    @Test
    void findByIdSecureReturnsOnlyCurrentTenantUser() {
        ContextManager.Tenant.set(tenantOne.toString());
        assertThat(userRepository.findByIdSecure(tenantOneUserId)).isPresent();

        ContextManager.Tenant.set(tenantTwo.toString());
        assertThat(userRepository.findByIdSecure(tenantOneUserId)).isEmpty();
    }

    @Test
    void findByIdSecureThrowsWhenTenantContextMissing() {
        ContextManager.Tenant.clear();
        assertThatThrownBy(() -> userRepository.findByIdSecure(tenantOneUserId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Tenant context is required");
    }

    @Test
    void findAllSecureFiltersByTenantContext() {
        ContextManager.Tenant.set(tenantOne.toString());
        List<User> tenantOneUsers = userRepository.findAllSecure();
        assertThat(tenantOneUsers)
                .hasSize(1)
                .allMatch(user -> user.getTenantId().equals(tenantOne));

        ContextManager.Tenant.set(tenantTwo.toString());
        List<User> tenantTwoUsers = userRepository.findAllSecure();
        assertThat(tenantTwoUsers)
                .hasSize(1)
                .allMatch(user -> user.getTenantId().equals(tenantTwo));
    }

    @Test
    void existsByIdSecureHonoursTenantBoundary() {
        ContextManager.Tenant.set(tenantOne.toString());
        assertThat(userRepository.existsByIdSecure(tenantOneUserId)).isTrue();
        assertThat(userRepository.existsByIdSecure(tenantTwoUserId)).isFalse();
    }

    @Test
    void countSecureReturnsTenantScopedCount() {
        ContextManager.Tenant.set(tenantOne.toString());
        assertThat(userRepository.countSecure()).isEqualTo(1);

        ContextManager.Tenant.set(tenantTwo.toString());
        assertThat(userRepository.countSecure()).isEqualTo(1);
    }

    @Test
    void deleteByIdSecureRemovesOnlyCurrentTenantEntity() {
        ContextManager.Tenant.set(tenantOne.toString());
        userRepository.deleteByIdSecure(tenantTwoUserId);
        assertThat(userRepository.existsByIdAndTenantId(tenantTwoUserId, tenantTwo)).isTrue();

        ContextManager.Tenant.set(tenantTwo.toString());
        userRepository.deleteByIdSecure(tenantTwoUserId);
        assertThat(userRepository.existsByIdAndTenantId(tenantTwoUserId, tenantTwo)).isFalse();
    }

    private User buildUser(UUID tenantId, String email, String username) {
        User user = User.builder()
                .tenantId(tenantId)
                .email(email)
                .username(username)
                .passwordHash("hash")
                .enabled(true)
                .locked(false)
                .build();
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setCreatedBy("system");
        user.setUpdatedBy("system");
        return user;
    }
}
