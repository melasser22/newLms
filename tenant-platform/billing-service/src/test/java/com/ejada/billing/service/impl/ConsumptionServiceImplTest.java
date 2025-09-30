package com.ejada.billing.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejada.billing.dto.ConsumptionType;
import com.ejada.billing.dto.ProductConsumption;
import com.ejada.billing.dto.ProductConsumptionStts;
import com.ejada.billing.dto.ProductSubscription;
import com.ejada.billing.dto.ProductSubscriptionStts;
import com.ejada.billing.dto.TrackProductConsumptionRq;
import com.ejada.billing.dto.TrackProductConsumptionRs;
import com.ejada.billing.mapper.ConsumptionResponseMapper;
import com.ejada.billing.mapper.UsageCounterMapper;
import com.ejada.billing.mapper.UsageEventMapper;
import com.ejada.billing.model.UsageEvent;
import com.ejada.billing.repository.UsageCounterRepository;
import com.ejada.billing.repository.UsageEventRepository;
import com.ejada.common.dto.ServiceResult;
import com.ejada.common.security.TokenHashUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class ConsumptionServiceImplTest {

  @Mock private UsageCounterRepository counterRepository;
  @Mock private UsageEventRepository eventRepository;
  @Mock private UsageCounterMapper counterMapper;
  @Mock private ConsumptionResponseMapper responseMapper;
  @Mock private UsageEventMapper eventMapper;

  private ConsumptionServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new ConsumptionServiceImpl(
        counterRepository,
        eventRepository,
        counterMapper,
        responseMapper,
        eventMapper,
        new ObjectMapper(),
        new NoOpTransactionManager());
  }

  @Test
  void trackProductConsumptionReturnsErrorWhenRepositoryFails() {
    UUID rqUid = UUID.randomUUID();
    TrackProductConsumptionRq request = new TrackProductConsumptionRq(
        11L,
        List.of(new ProductSubscription(
            21L,
            31L,
            null,
            null,
            List.of(new ProductConsumption(ConsumptionType.TRANSACTION))
        )));

    when(counterRepository.findByExtSubscriptionIdAndConsumptionTypCd(31L, "TRANSACTION"))
        .thenReturn(Optional.empty());
    when(counterRepository.save(any())).thenThrow(new RuntimeException("db down"));
    UsageEvent auditEvent = UsageEvent.builder().usageEventId(2L).build();
    when(eventMapper.build(any(), any(), any(), any(), any(), any(), any())).thenReturn(auditEvent);
    when(eventRepository.save(any())).thenReturn(auditEvent);

    ServiceResult<TrackProductConsumptionRs> result = service.trackProductConsumption(rqUid, "token", request);

    assertThat(result.success()).isFalse();
    assertThat(result.statusCode()).isEqualTo("EINT000");
    assertThat(result.statusDetails()).contains("Unexpected Error");
    assertThat(result.debugId()).isNotBlank();

    verify(eventMapper).build(eq(rqUid), any(), any(), eq(11L), eq("EINT000"), eq("Unexpected Error"), any());
    verify(eventRepository, times(1)).save(auditEvent);
  }

  @Test
  void trackProductConsumptionReturnsSuccessAndAuditsEvent() {
    UUID rqUid = UUID.randomUUID();
    TrackProductConsumptionRq request = new TrackProductConsumptionRq(
        10L,
        List.of(new ProductSubscription(
            20L,
            30L,
            null,
            null,
            List.of(new ProductConsumption(ConsumptionType.TRANSACTION))
        )));

    when(counterRepository.findByExtSubscriptionIdAndConsumptionTypCd(30L, "TRANSACTION"))
        .thenReturn(Optional.empty());
    when(counterRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(counterMapper.toDto(any())).thenReturn(new ProductConsumptionStts(ConsumptionType.TRANSACTION, 0L, null));

    ProductSubscriptionStts subscriptionStts = new ProductSubscriptionStts(20L, 30L, List.of());
    when(responseMapper.toSubscriptionStts(eq(20L), eq(30L), any())).thenReturn(subscriptionStts);
    TrackProductConsumptionRs response = new TrackProductConsumptionRs(10L, List.of(subscriptionStts));
    when(responseMapper.toResponse(eq(10L), any())).thenReturn(response);

    UsageEvent usageEvent = UsageEvent.builder().usageEventId(1L).build();
    when(eventMapper.build(any(), any(), any(), any(), any(), any(), any())).thenReturn(usageEvent);
    when(eventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    ServiceResult<TrackProductConsumptionRs> result = service.trackProductConsumption(rqUid, "token", request);

    assertThat(result.success()).isTrue();
    assertThat(result.payload()).isEqualTo(response);

    ArgumentCaptor<String> tokenHashCaptor = ArgumentCaptor.forClass(String.class);
    verify(eventMapper).build(eq(rqUid), tokenHashCaptor.capture(), any(), eq(10L), eq("I000000"), eq("Successful Operation"), eq(null));
    assertThat(tokenHashCaptor.getValue()).isEqualTo(TokenHashUtils.sha256("token"));
    verify(eventRepository, times(1)).save(usageEvent);
  }

  private static final class NoOpTransactionManager implements PlatformTransactionManager {
    @Override
    public TransactionStatus getTransaction(TransactionDefinition definition) {
      return new SimpleTransactionStatus();
    }

    @Override
    public void commit(TransactionStatus status) {
      // no-op
    }

    @Override
    public void rollback(TransactionStatus status) {
      // no-op
    }
  }
}
