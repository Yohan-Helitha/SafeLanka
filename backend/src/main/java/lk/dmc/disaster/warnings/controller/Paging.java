package lk.dmc.disaster.warnings.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Turns the page and size query parameters into a safe {@link Pageable}. */
final class Paging {

  static final int DEFAULT_SIZE = 20;
  static final int MAX_SIZE = 100;

  private Paging() {}

  /** A negative page becomes the first page; a size is kept between 1 and {@value #MAX_SIZE}. */
  static Pageable of(int page, int size, Sort sort) {
    return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_SIZE), sort);
  }
}
