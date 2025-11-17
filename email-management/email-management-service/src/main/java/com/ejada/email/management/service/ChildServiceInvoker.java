package com.ejada.email.management.service;

import com.ejada.email.management.exception.ChildServiceException;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ChildServiceInvoker {

  private final RestClient restClient;

  public ChildServiceInvoker(RestClient restClient) {
    this.restClient = restClient.mutate().build();
  }

  public <T> T get(URI baseUrl, String path, Class<T> responseType) {
    try {
      return restClient
          .get()
          .uri(baseUrl.resolve(path))
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(responseType);
    } catch (Exception ex) {
      throw new ChildServiceException("Failed to call child service: " + baseUrl, ex);
    }
  }

  public <B, T> T post(URI baseUrl, String path, B body, Class<T> responseType) {
    try {
      return restClient
          .post()
          .uri(baseUrl.resolve(path))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .body(body)
          .retrieve()
          .body(responseType);
    } catch (Exception ex) {
      throw new ChildServiceException("Failed to call child service: " + baseUrl, ex);
    }
  }
}
