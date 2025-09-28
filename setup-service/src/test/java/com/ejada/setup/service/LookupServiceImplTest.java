package com.ejada.setup.service;

import com.ejada.common.dto.BaseResponse;
import com.ejada.setup.dto.LookupCreateRequest;
import com.ejada.setup.dto.LookupResponse;
import com.ejada.setup.repository.LookupRepository;
import com.ejada.setup.service.impl.LookupServiceImpl;
import com.ejada.setup.model.Lookup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cache.CacheManager;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class LookupServiceImplTest {

    @Mock
    private LookupRepository lookupRepository;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private LookupServiceImpl service;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllLookups_ok() {
        Lookup entity = Lookup.builder().lookupItemId(1).lookupItemCd("CODE").lookupGroupCode("GROUP").build();
        when(lookupRepository.findAll()).thenReturn(Collections.singletonList(entity));
        BaseResponse<List<LookupResponse>> resp = service.getAllLookups();
        assertNotNull(resp);
        assertEquals(1, resp.getData().size());
    }

    @Test
    void addLookup_convertsRequest() {
        LookupCreateRequest req = new LookupCreateRequest(1, "CODE", null, null, "GROUP", null, Boolean.TRUE, null, null);
        Lookup saved = Lookup.builder().lookupItemId(1).lookupItemCd("CODE").lookupGroupCode("GROUP").build();
        when(lookupRepository.save(any(Lookup.class))).thenReturn(saved);
        BaseResponse<LookupResponse> resp = service.add(req);
        assertNotNull(resp.getData());
        assertEquals("CODE", resp.getData().lookupItemCd());
    }
}
