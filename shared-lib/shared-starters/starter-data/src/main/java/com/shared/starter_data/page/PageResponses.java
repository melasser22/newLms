package com.shared.starter_data.page;

import java.util.function.Function;
import org.springframework.data.domain.Page;

public final class PageResponses {
  private PageResponses() {}

  public static <T> PageResponse<T> of(Page<T> page) {
    return new PageResponse<>(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isFirst(),
        page.isLast()
    );
  }

  // entities -> DTOs using Page.map to avoid type-inference issues
  public static <E, D> PageResponse<D> of(Page<E> page, Function<? super E, ? extends D> mapper) {
    Page<D> mapped = page.map(mapper);
    return of(mapped);
  }
}
