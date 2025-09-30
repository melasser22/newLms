package com.ejada.setup.service;

import com.ejada.setup.mapper.SystemParameterMapper;
import com.ejada.setup.model.SystemParameter;
import com.ejada.setup.repository.SystemParameterRepository;
import com.ejada.setup.service.impl.SystemParameterServiceImpl;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mapstruct.factory.Mappers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemParameterServiceImplTest {

  @Mock SystemParameterRepository repository;
  private SystemParameterMapper mapper;
  private SystemParameterServiceImpl service;

  @BeforeEach
  void setUp() {
    mapper = Mappers.getMapper(SystemParameterMapper.class);
    service = new SystemParameterServiceImpl(repository, mapper);
  }

  @Test
  void getByKey_ok() {
    SystemParameter entity = new SystemParameter();
    entity.setParamId(1);
    entity.setParamKey("SITE_NAME");
    entity.setParamValue("Portal");
    entity.setParamGroup("UI");
    entity.setIsActive(true);
    when(repository.findByParamKey("SITE_NAME"))
        .thenReturn(Optional.of(entity));

    var res = service.getByKey("SITE_NAME");
    assertNotNull(res);
    assertEquals("SITE_NAME", res.getData().getParamKey());
  }
}
