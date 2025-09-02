package com.ejada.setup.service;

import com.ejada.common.dto.BaseResponse;
import com.ejada.setup.model.Lookup;
import com.ejada.setup.repository.LookupRepository;
import com.ejada.setup.service.impl.LookupServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class LookupServiceImplTest {

    @Mock
    private LookupRepository lookupRepository;

    @InjectMocks
    private LookupServiceImpl service;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllLookups_ok() {
        when(lookupRepository.findAll()).thenReturn(Collections.singletonList(new Lookup()));
        BaseResponse<List<Lookup>> resp = service.getAllLookups();
        assertNotNull(resp);
    }
}
