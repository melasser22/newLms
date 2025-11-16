package com.ejada.management.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejada.management.exception.ChildServiceException;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

class ChildServiceInvokerTest {

  private RestClient baseClient;
  private RestClient.Builder builder;
  private RestClient restClient;
  private ChildServiceInvoker invoker;

  @BeforeEach
  void setUp() {
    baseClient = mock(RestClient.class);
    builder = mock(RestClient.Builder.class);
    restClient = mock(RestClient.class);

    when(baseClient.mutate()).thenReturn(builder);
    when(builder.build()).thenReturn(restClient);

    invoker = new ChildServiceInvoker(baseClient);
  }

  @Test
  void getShouldInvokeRestClientAndReturnResponse() {
    RestClient.RequestHeadersUriSpec<?> requestSpec = mock(RestClient.RequestHeadersUriSpec.class);
    RestClient.RequestHeadersSpec<?> requestHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
    RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

    doReturn(requestSpec).when(restClient).get();
    doReturn(requestHeadersSpec).when(requestSpec).uri(any(URI.class));
    doReturn(requestHeadersSpec).when(requestHeadersSpec).accept(MediaType.APPLICATION_JSON);
    doReturn(responseSpec).when(requestHeadersSpec).retrieve();
    when(responseSpec.body(String.class)).thenReturn("response-body");

    URI baseUrl = URI.create("https://child.test/api/");
    String result = invoker.get(baseUrl, "resource", String.class);

    ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
    verify(requestSpec).uri(uriCaptor.capture());

    assertThat(uriCaptor.getValue()).isEqualTo(baseUrl.resolve("resource"));
    assertThat(result).isEqualTo("response-body");
  }

  @Test
  void postShouldWrapRestClientExceptions() {
    RestClient.RequestBodyUriSpec requestBodySpec = mock(RestClient.RequestBodyUriSpec.class);
    RestClient.RequestBodySpec requestSpec = mock(RestClient.RequestBodySpec.class);
    RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

    when(restClient.post()).thenReturn(requestBodySpec);
    when(requestBodySpec.uri(any(URI.class))).thenReturn(requestSpec);
    when(requestSpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestSpec);
    when(requestSpec.accept(MediaType.APPLICATION_JSON)).thenReturn(requestSpec);
    when(requestSpec.body(any())).thenReturn(requestSpec);
    when(requestSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.body(eq(String.class))).thenThrow(new RestClientException("failure"));

    URI baseUrl = URI.create("https://child.test/api/");

    assertThatThrownBy(() -> invoker.post(baseUrl, "submit", "payload", String.class))
        .isInstanceOf(ChildServiceException.class)
        .hasMessageContaining("https://child.test/api/");
  }
}
