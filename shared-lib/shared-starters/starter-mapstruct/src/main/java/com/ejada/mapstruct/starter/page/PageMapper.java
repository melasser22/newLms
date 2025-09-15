
package com.ejada.mapstruct.starter.page;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public interface PageMapper {

  default <E, D> PageDto<D> toPageDto(Page<E> page, Function<E, D> elementMapper) {
    List<D> content = page.getContent().stream().map(elementMapper).toList();
    return new PageDto<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
  }
}
