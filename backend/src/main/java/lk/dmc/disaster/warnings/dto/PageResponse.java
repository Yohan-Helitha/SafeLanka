package lk.dmc.disaster.warnings.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/** One page of results, without leaking Spring's own Page type to clients. */
public record PageResponse<T>(
    List<T> content, int page, int size, long totalElements, int totalPages) {

  /** Maps a Spring page into a page response, converting each item. */
  public static <S, T> PageResponse<T> of(Page<S> page, Function<S, T> mapper) {
    return new PageResponse<>(
        page.getContent().stream().map(mapper).toList(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
