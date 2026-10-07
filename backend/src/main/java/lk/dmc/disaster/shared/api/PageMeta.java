package lk.dmc.disaster.shared.api;

import org.springframework.data.domain.Page;

/** Paging details returned beside a list: {@code meta} in the success envelope. */
public record PageMeta(int page, int size, long totalElements, int totalPages) {

  public static PageMeta of(Page<?> page) {
    return new PageMeta(
        page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
  }
}
