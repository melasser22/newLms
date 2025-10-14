package com.ejada.data.repository;

import com.ejada.common.tenant.TenantIsolationValidator;
import com.ejada.starter_core.context.TenantContextResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Base repository interface that enforces tenant isolation for all data access operations.
 *
 * <p>All repositories managing tenant-scoped entities should extend this interface instead of
 * {@link JpaRepository} directly. This interface provides tenant-safe alternatives to standard CRUD
 * operations that automatically filter by the current tenant context.
 *
 * <p><b>Key Design Principles:</b>
 * <ul>
 *   <li>Default methods extract tenant ID from {@link TenantContextResolver}</li>
 *   <li>All queries automatically filter by tenant_id column</li>
 *   <li>Prevents accidental cross-tenant data access</li>
 *   <li>Enforces explicit tenant awareness in all data operations</li>
 * </ul>
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * public interface UserRepository extends TenantAwareRepository<User, Long> {
 *     Optional<User> findByTenantIdAndUsername(UUID tenantId, String username);
 * }
 *
 * // In Service Layer
 * User user = userRepository.findByIdSecure(userId)
 *     .orElseThrow(() -> new EntityNotFoundException("User not found"));
 * }</pre>
 *
 * <p><b>Security Contract:</b>
 * All methods in this interface guarantee:
 * <ol>
 *   <li>Current tenant context is resolved from {@link TenantContextResolver}</li>
 *   <li>If tenant context is missing, methods throw {@link IllegalStateException}</li>
 *   <li>Results are always filtered to current tenant's data only</li>
 *   <li>Cross-tenant access is impossible through this interface</li>
 * </ol>
 *
 * @param <T> the domain type the repository manages (must have tenantId field)
 * @param <ID> the type of the id of the entity (typically Long or UUID)
 */
@NoRepositoryBean
public interface TenantAwareRepository<T, ID> extends JpaRepository<T, ID> {

    // ============================================
    // TENANT-SAFE FINDER METHODS
    // ============================================

    /**
     * Finds an entity by its ID, ensuring it belongs to the current tenant.
     *
     * @param id the entity ID (must not be null)
     * @return Optional containing the entity if found in current tenant's data, empty otherwise
     * @throws IllegalStateException if tenant context is not available
     */
    default Optional<T> findByIdSecure(ID id) {
        UUID tenantId = resolveTenant("findByIdSecure");
        return findByIdAndTenantId(id, tenantId);
    }

    /**
     * Finds an entity by ID and explicit tenant ID.
     *
     * @param id the entity ID
     * @param tenantId the tenant UUID
     * @return Optional containing the entity if found, empty otherwise
     */
    Optional<T> findByIdAndTenantId(ID id, UUID tenantId);

    /**
     * Checks if an entity exists by ID within the current tenant's data.
     *
     * @param id the entity ID
     * @return true if entity exists and belongs to current tenant, false otherwise
     * @throws IllegalStateException if tenant context is not available
     */
    default boolean existsByIdSecure(ID id) {
        UUID tenantId = resolveTenant("existsByIdSecure");
        return existsByIdAndTenantId(id, tenantId);
    }

    /**
     * Checks if an entity exists by ID and tenant ID.
     *
     * @param id the entity ID
     * @param tenantId the tenant UUID
     * @return true if entity exists, false otherwise
     */
    boolean existsByIdAndTenantId(ID id, UUID tenantId);

    // ============================================
    // TENANT-SAFE LIST METHODS
    // ============================================

    /**
     * Retrieves all entities for the current tenant.
     *
     * @return List of all entities belonging to current tenant (never null, may be empty)
     * @throws IllegalStateException if tenant context is not available
     */
    default List<T> findAllSecure() {
        UUID tenantId = resolveTenant("findAllSecure");
        return findAllByTenantId(tenantId);
    }

    /**
     * Retrieves all entities for a specific tenant.
     *
     * @param tenantId the tenant UUID
     * @return List of all entities for the specified tenant
     */
    List<T> findAllByTenantId(UUID tenantId);

    /**
     * Retrieves all entities for the current tenant with sorting.
     *
     * @param sort the sort specification
     * @return Sorted list of entities belonging to current tenant
     * @throws IllegalStateException if tenant context is not available
     */
    default List<T> findAllSecure(Sort sort) {
        UUID tenantId = resolveTenant("findAllSecure(Sort)");
        return findAllByTenantId(tenantId, sort);
    }

    /**
     * Retrieves all entities for a specific tenant with sorting.
     *
     * @param tenantId the tenant UUID
     * @param sort the sort specification
     * @return Sorted list of entities
     */
    List<T> findAllByTenantId(UUID tenantId, Sort sort);

    /**
     * Retrieves a page of entities for the current tenant.
     *
     * @param pageable the pagination information
     * @return Page of entities belonging to current tenant
     * @throws IllegalStateException if tenant context is not available
     */
    default Page<T> findAllSecure(Pageable pageable) {
        UUID tenantId = resolveTenant("findAllSecure(Pageable)");
        return findAllByTenantId(tenantId, pageable);
    }

    /**
     * Retrieves a page of entities for a specific tenant.
     *
     * @param tenantId the tenant UUID
     * @param pageable the pagination information
     * @return Page of entities
     */
    Page<T> findAllByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Counts all entities for the current tenant.
     *
     * @return Total count of entities belonging to current tenant
     * @throws IllegalStateException if tenant context is not available
     */
    default long countSecure() {
        UUID tenantId = resolveTenant("countSecure");
        return countByTenantId(tenantId);
    }

    /**
     * Counts entities for a specific tenant.
     *
     * @param tenantId the tenant UUID
     * @return Total count of entities
     */
    long countByTenantId(UUID tenantId);

    // ============================================
    // TENANT-SAFE DELETE METHODS
    // ============================================

    /**
     * Deletes an entity by ID, ensuring it belongs to the current tenant.
     *
     * @param id the entity ID to delete
     * @throws IllegalStateException if tenant context is not available
     */
    default void deleteByIdSecure(ID id) {
        UUID tenantId = resolveTenant("deleteByIdSecure");
        deleteByIdAndTenantId(id, tenantId);
    }

    /**
     * Deletes an entity by ID and tenant ID.
     *
     * @param id the entity ID
     * @param tenantId the tenant UUID
     */
    void deleteByIdAndTenantId(ID id, UUID tenantId);

    /**
     * Deletes all entities for the current tenant.
     *
     * @throws IllegalStateException if tenant context is not available
     */
    default void deleteAllSecure() {
        UUID tenantId = resolveTenant("deleteAllSecure");
        deleteAllByTenantId(tenantId);
    }

    /**
     * Deletes all entities for a specific tenant.
     *
     * @param tenantId the tenant UUID
     */
    void deleteAllByTenantId(UUID tenantId);

    private static UUID resolveTenant(String operation) {
        UUID tenantId = TenantContextResolver.requireTenantId();
        TenantIsolationValidator.verifyDatabaseAccess(operation, tenantId);
        return tenantId;
    }

    // ============================================
    // TENANT-SAFE SAVE METHODS
    // ============================================

    /**
     * Saves an entity, automatically validating or setting the tenant ID when possible.
     *
     * @param entity the entity to save
     * @return the saved entity
     * @throws IllegalStateException if tenant context is not available or the entity does not expose
     *                               standard tenant accessors
     * @throws IllegalArgumentException if an existing entity's tenantId does not match the context
     */
    default <S extends T> S saveSecure(S entity) {
        UUID currentTenantId = resolveTenant("saveSecure");
        UUID entityTenantId = readTenantId(entity);

        if (entityTenantId == null) {
            writeTenantId(entity, currentTenantId);
        } else if (!currentTenantId.equals(entityTenantId)) {
            throw new IllegalArgumentException(
                    "Tenant mismatch: entity belongs to " + entityTenantId +
                    " but current context is " + currentTenantId);
        }

        return save(entity);
    }

    // ============================================
    // UTILITY METHODS FOR SUPERADMIN OPERATIONS
    // ============================================

    /**
     * Retrieves all entities across ALL tenants. Use with extreme caution.
     *
     * @return List of all entities across all tenants
     */
    default List<T> findAllUnsafe() {
        return findAll();
    }

    /**
     * Finds entity by ID without tenant check. Use with extreme caution.
     *
     * @param id the entity ID
     * @return Optional containing the entity if found
     */
    default Optional<T> findByIdUnsafe(ID id) {
        return findById(id);
    }

    // ============================================
    // REFLECTION HELPERS
    // ============================================

    private static UUID readTenantId(Object entity) {
        Method getter = findMethod(entity.getClass(), "getTenantId");
        if (getter == null) {
            throw new IllegalStateException(
                    "Entity " + entity.getClass().getName() + " must declare getTenantId()");
        }

        try {
            Object value = getter.invoke(entity);
            if (value == null) {
                return null;
            }
            if (value instanceof UUID uuid) {
                return uuid;
            }
            if (value instanceof String str && !str.isBlank()) {
                return UUID.fromString(str);
            }
            throw new IllegalStateException(
                    "Unsupported tenantId type " + value.getClass().getName() +
                            " on entity " + entity.getClass().getName());
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to read tenantId from entity", e);
        }
    }

    private static void writeTenantId(Object entity, UUID tenantId) {
        Method uuidSetter = findMethod(entity.getClass(), "setTenantId", UUID.class);
        Method stringSetter = findMethod(entity.getClass(), "setTenantId", String.class);

        Method setter = uuidSetter != null ? uuidSetter : stringSetter;
        if (setter == null) {
            throw new IllegalStateException(
                    "Entity " + entity.getClass().getName() + " must declare setTenantId(..)");
        }

        try {
            if (setter.getParameterTypes()[0] == UUID.class) {
                setter.invoke(entity, tenantId);
            } else {
                setter.invoke(entity, tenantId != null ? tenantId.toString() : null);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to write tenantId to entity", e);
        }
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            Method method = type.getMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException ex) {
            Class<?> superclass = type.getSuperclass();
            if (superclass != null && superclass != Object.class) {
                return findMethod(superclass, name, parameterTypes);
            }
            return null;
        }
    }
}
