package com.ejada.catalog.service;

import com.ejada.catalog.dto.*;
import com.ejada.catalog.mapper.AddonMapper;
import com.ejada.catalog.model.Addon;
import com.ejada.catalog.repository.AddonRepository;
import com.ejada.catalog.service.impl.AddonServiceImpl;
import com.ejada.common.dto.BaseResponse;
import com.ejada.common.exception.NotFoundException;
import com.ejada.common.exception.DuplicateResourceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("AddonServiceImpl tests")
class AddonServiceImplTest {

    @Mock AddonRepository repo;
    @Mock AddonMapper mapper;

    @InjectMocks AddonServiceImpl service;

    AddonServiceImplTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("should create addon successfully")
    void create_shouldCreateAddon() {
        AddonCreateReq req = new AddonCreateReq("CD1", "Addon 1", "AR", "desc", "CAT", true);
        when(repo.existsByAddonCd("CD1")).thenReturn(false);
        Addon entity = new Addon();
        when(mapper.toEntity(req)).thenReturn(entity);
        when(repo.save(entity)).thenReturn(entity);
        AddonRes resDto = new AddonRes(1, "CD1", "Addon 1", "AR", "desc", "CAT", true, false, null, null);
        when(mapper.toRes(entity)).thenReturn(resDto);

        BaseResponse<AddonRes> res = service.create(req);

        assertEquals("Addon created", res.getMessage());
        assertEquals(resDto, res.getData());
    }

    @Test
    @DisplayName("should throw DuplicateResourceException when addonCd exists")
    void create_shouldThrowOnDuplicateCd() {
        AddonCreateReq req = new AddonCreateReq("CD1", "Addon", "AR", null, "CAT", true);
        when(repo.existsByAddonCd("CD1")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.create(req));
    }

    @Test
    @DisplayName("should return addon by id if exists")
    void get_shouldReturnAddon() {
        Addon entity = new Addon();
        when(repo.findById(1)).thenReturn(Optional.of(entity));
        AddonRes resDto = new AddonRes(1, "CD1", "Addon 1", "AR", "desc", "CAT", true, false, null, null);
        when(mapper.toRes(entity)).thenReturn(resDto);

        BaseResponse<AddonRes> res = service.get(1);

        assertEquals(resDto, res.getData());
        assertEquals("OK", res.getMessage());
    }

    @Test
    @DisplayName("should throw NotFoundException when addon not found")
    void get_shouldThrowWhenNotFound() {
        when(repo.findById(1)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.get(1));
    }

    @Test
    @DisplayName("should list addons with no category filter")
    void list_shouldReturnPageWithoutCategory() {
        List<Addon> entities = List.of(new Addon(), new Addon());
        Page<Addon> page = new PageImpl<>(entities);
        when(repo.findByIsDeletedFalse(any(Pageable.class))).thenReturn(page);
        when(mapper.toRes(any(Addon.class))).thenReturn(new AddonRes(1, "CD", "Addon", "AR", null, null, true, false, null, null));

        BaseResponse<Page<AddonRes>> res = service.list(null, Pageable.unpaged());

        assertEquals(2, res.getData().getTotalElements());
    }

    @Test
    @DisplayName("should update addon when id exists")
    void update_shouldUpdateAddon() {
        Addon entity = new Addon();
        when(repo.findById(1)).thenReturn(Optional.of(entity));
        AddonUpdateReq req = new AddonUpdateReq("CD2", "New En", "New Ar", "desc", "CAT", true);
        AddonRes resDto = new AddonRes(1, "CD2", "New En", "New Ar", "desc", "CAT", true, false, null, null);
        doNothing().when(mapper).update(entity, req);
        when(mapper.toRes(entity)).thenReturn(resDto);

        BaseResponse<AddonRes> res = service.update(1, req);

        verify(mapper).update(entity, req);
        assertEquals("Addon updated", res.getMessage());
        assertEquals(resDto, res.getData());
    }

    @Test
    @DisplayName("should throw NotFoundException when updating missing addon")
    void update_shouldThrowWhenMissing() {
        when(repo.findById(1)).thenReturn(Optional.empty());
        AddonUpdateReq req = new AddonUpdateReq(null, null, null, null, null, null);
        assertThrows(NotFoundException.class, () -> service.update(1, req));
    }

    @Test
    @DisplayName("should soft delete addon when id exists")
    void softDelete_shouldSoftDelete() {
        Addon entity = new Addon();
        entity.setIsDeleted(false);
        when(repo.findById(1)).thenReturn(Optional.of(entity));

        BaseResponse<Void> res = service.softDelete(1);

        assertTrue(entity.getIsDeleted());
        assertEquals("Addon deleted", res.getMessage());
        assertNull(res.getData());
    }

    @Test
    @DisplayName("should throw NotFoundException when soft deleting missing addon")
    void softDelete_shouldThrowWhenMissing() {
        when(repo.findById(1)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.softDelete(1));
    }
}

