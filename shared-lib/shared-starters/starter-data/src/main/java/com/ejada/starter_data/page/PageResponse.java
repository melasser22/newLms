package com.ejada.starter_data.page;

import java.util.List;

public final class PageResponse<T> {
  private final List<T> items;
  private final int page;
  private final int size;
  private final long totalElements;
  private final int totalPages;
  private final boolean first;
  private final boolean last;

  public PageResponse(
      List<T> items, int page, int size,
      long totalElements, int totalPages,
      boolean first, boolean last) {
    this.items = items;
    this.page = page;
    this.size = size;
    this.totalElements = totalElements;
    this.totalPages = totalPages;
    this.first = first;
    this.last = last;
  }

  public List<T> getItems() { return items; }
  public int getPage() { return page; }
  public int getSize() { return size; }
  public long getTotalElements() { return totalElements; }
  public int getTotalPages() { return totalPages; }
  public boolean isFirst() { return first; }
  public boolean isLast() { return last; }
}
