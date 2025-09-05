package com.ejada.setup.service;

import com.ejada.setup.repository.SystemParameterRepository;
import com.ejada.setup.service.impl.SystemParameterServiceImpl;
import com.ejada.setup.model.SystemParameter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemParameterServiceImplTest {

  @Mock SystemParameterRepository repository;
  @InjectMocks SystemParameterServiceImpl service;

  @Test
  void getByKey_ok() {
    when(repository.findByParamKey("SITE_NAME"))
        .thenReturn(Optional.of(new SystemParameter(/* init fields */)));

    var res = service.getByKey("SITE_NAME");
    assertNotNull(res);
  }
}
