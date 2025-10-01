package com.ejada.billing.controller;

import static com.ejada.testsupport.assertions.ResponseAssertions.assertThatServiceResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.ejada.billing.dto.ConsumptionType;
import com.ejada.billing.dto.ProductConsumption;
import com.ejada.billing.dto.ProductSubscription;
import com.ejada.billing.dto.TrackProductConsumptionRq;
import com.ejada.billing.dto.TrackProductConsumptionRs;
import com.ejada.common.exception.ServiceResultException;
import com.ejada.billing.service.ConsumptionService;
import com.ejada.common.dto.ServiceResult;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ConsumptionControllerTest {

    @Mock
    private ConsumptionService service;

    private ConsumptionController controller;

    @BeforeEach
    void setUp() {
        controller = new ConsumptionController(service);
    }

    @Test
    void trackReturnsOkWhenServiceSucceeds() {
        TrackProductConsumptionRq request = request();
        TrackProductConsumptionRs payload = new TrackProductConsumptionRs(10L, List.of());
        when(service.trackProductConsumption(any(), any(), eq(request))).thenReturn(ServiceResult.ok(payload));

        ResponseEntity<ServiceResult<TrackProductConsumptionRs>> response =
                controller.track(UUID.randomUUID(), "token", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThatServiceResult(response.getBody())
                .isSuccess()
                .hasPayloadSatisfying(result -> assertThat(result.productId()).isEqualTo(10L));
    }

    @Test
    void trackMapsBusinessErrorToBadRequest() {
        TrackProductConsumptionRq request = request();
        ServiceResult<TrackProductConsumptionRs> failure =
                ServiceResult.error(null, "EBUS001", "Business validation", List.of("quota exceeded"));
        when(service.trackProductConsumption(any(), any(), eq(request))).thenReturn(failure);

        ResponseEntity<ServiceResult<TrackProductConsumptionRs>> response =
                controller.track(UUID.randomUUID(), "token", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatServiceResult(response.getBody())
                .isFailure()
                .hasStatusCode("EBUS001");
    }

    @Test
    void trackMapsExceptionToInternalServerError() {
        TrackProductConsumptionRq request = request();
        ServiceResult<TrackProductConsumptionRs> failure =
                ServiceResult.error(null, "EINT999", "Unexpected", List.of("boom"));
        doThrow(new ServiceResultException(failure))
                .when(service)
                .trackProductConsumption(any(), any(), eq(request));

        ResponseEntity<ServiceResult<TrackProductConsumptionRs>> response =
                controller.track(UUID.randomUUID(), "token", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThatServiceResult(response.getBody())
                .isFailure()
                .hasStatusCode("EINT999");
    }

    private TrackProductConsumptionRq request() {
        ProductConsumption consumption = new ProductConsumption(ConsumptionType.TRANSACTION);
        ProductSubscription subscription =
                new ProductSubscription(100L, 200L, "2023-01-01", "2023-12-31", List.of(consumption));
        return new TrackProductConsumptionRq(10L, List.of(subscription));
    }
}
