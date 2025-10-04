package com.ejada.sec.repository;

import com.ejada.common.context.ContextManager;
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

@DataJpaTest
class UserRepositoryTenantIsolationTest {

    @Autowired
    private UserRepository userRepository;

    private UUID tenantOne;
    private UUID tenantTwo;
    private Long tenantOneUserId;

    @BeforeEach
    void setUp() {
        tenantOne = UUID.randomUUID();
        tenantTwo = UUID.randomUUID();

        tenantOneUserId = userRepository.save(buildUser(tenantOne, "tenant1@example.com", "tenant1"))
                .getId();
        userRepository.save(buildUser(tenantTwo, "tenant2@example.com", "tenant2"));
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
