package com.ejada.push.management.service;

import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ChildServiceInvoker {

  private final RestClient restClient;

  public ChildServiceInvoker(RestClient restClient) {
    this.restClient = restClient.mutate().build();
  }

  public <T> T get(URI baseUrl, String path, Class<T> responseType) {
    return restClient
        .get()
        .uri(baseUrl.resolve(path))
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .body(responseType);
  }

  public <B, T> T post(URI baseUrl, String path, B body, Class<T> responseType) {
    return restClient
        .post()
        .uri(baseUrl.resolve(path))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .body(responseType);
  }
}
