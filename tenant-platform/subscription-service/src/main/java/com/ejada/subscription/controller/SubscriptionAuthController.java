package com.ejada.subscription.controller;

import com.ejada.subscription.dto.auth.GetSubscriptionTokenRq;
import com.ejada.subscription.dto.auth.GetSubscriptionTokenRs;
import com.ejada.subscription.dto.auth.ServiceResult;
import com.ejada.subscription.service.SubscriptionAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/subscription", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class SubscriptionAuthController {

  private static final Logger log = LoggerFactory.getLogger(SubscriptionAuthController.class);
  private static final Pattern RQUID_PATTERN =
      Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

  private final SubscriptionAuthService authService;

  @Operation(
      summary = "Generate subscription JWT token",
      description = "Authenticates a subscription user and returns a signed JWT token",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful Operation",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ServiceResult.class))),
        @ApiResponse(responseCode = "400", description = "Validation failure"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "500", description = "Unexpected error")
      })
  @PostMapping(value = "/get-token", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ServiceResult<GetSubscriptionTokenRs>> getToken(
      @Parameter(
              in = ParameterIn.HEADER,
              name = "rqUID",
              required = true,
              description = "Request unique identifier in UUID format",
              example = "c73bcdcc-2669-4bf6-81d3-e4ae73fb11fd")
          @RequestHeader("rqUID")
          final String rqUid,
      @Valid @RequestBody final GetSubscriptionTokenRq request) {

    log.info("Processing subscription token request rqUID={}", rqUid);
    if (!isValidRqUid(rqUid)) {
      log.warn("Invalid rqUID provided rqUID={}", rqUid);
      ServiceResult<GetSubscriptionTokenRs> error =
          ServiceResult.failure(rqUid, List.of("Invalid rqUID format"));
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    String token = authService.authenticate(request.loginName(), request.password());
    ServiceResult<GetSubscriptionTokenRs> response =
        ServiceResult.success(rqUid, new GetSubscriptionTokenRs(token));
    log.info("Successfully issued subscription token rqUID={}", rqUid);
    return ResponseEntity.ok(response);
  }

  private boolean isValidRqUid(final String rqUid) {
    return rqUid != null && RQUID_PATTERN.matcher(rqUid).matches();
  }
}
