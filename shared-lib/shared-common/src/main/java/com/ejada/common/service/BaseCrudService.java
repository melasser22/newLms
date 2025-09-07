package com.ejada.common.service;

import com.ejada.common.dto.BaseResponse;
import com.ejada.common.exception.DuplicateResourceException;
import com.ejada.common.exception.ResourceNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

/**
 * Generic CRUD service providing common create, read, update and soft-delete operations.
 *
 * @param <E>  entity type
 * @param <ID> identifier type
 * @param <C>  create DTO type
 * @param <U>  update DTO type
 * @param <R>  response DTO type
 */
public abstract class BaseCrudService<E, ID, C, U, R> {

    /** Repository for the entity. */
    protected abstract JpaRepository<E, ID> getRepository();

    /** Check for a unique constraint prior to creation. */
    protected abstract boolean existsByUniqueField(C dto);

    /** Map a create DTO to an entity instance. */
    protected abstract E mapToEntity(C dto);

    /** Map updated values from a DTO to an existing entity. */
    protected abstract void updateEntity(E entity, U dto);

    /** Map an entity to its response DTO. */
    protected abstract R mapToDto(E entity);

    /** Human friendly name of the entity for messages. */
    protected String getEntityName() { return "Resource"; }

    @Transactional
    public BaseResponse<R> create(C dto) {
        if (existsByUniqueField(dto)) {
            throw new DuplicateResourceException(getEntityName() + " already exists");
        }
        E entity = mapToEntity(dto);
        E saved = getRepository().save(entity);
        return BaseResponse.success(getEntityName() + " created", mapToDto(saved));
    }

    @Transactional(readOnly = true)
    public BaseResponse<R> get(ID id) {
        E entity = getRepository().findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(getEntityName(), String.valueOf(id)));
        return BaseResponse.success("OK", mapToDto(entity));
    }

    @Transactional
    public BaseResponse<R> update(ID id, U dto) {
        E entity = getRepository().findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(getEntityName(), String.valueOf(id)));
        updateEntity(entity, dto);
        return BaseResponse.success(getEntityName() + " updated", mapToDto(entity));
    }

    @Transactional
    public BaseResponse<Void> softDelete(ID id) {
        E entity = getRepository().findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(getEntityName(), String.valueOf(id)));
        try {
            Method m = entity.getClass().getMethod("setIsDeleted", Boolean.class);
            m.invoke(entity, Boolean.TRUE);
        } catch (Exception ex) {
            throw new IllegalStateException("Soft delete not supported for " + getEntityName(), ex);
        }
        return BaseResponse.success(getEntityName() + " deleted", null);
    }
}

