package lk.dmc.disaster.shared.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import org.springframework.data.domain.Page;

/** Success envelope: {@code { "data": ..., "meta"? }}. {@code meta} is present for pages only. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(T data, PageMeta meta) {

  public static <T> ApiResponse<T> of(T data) {
    return new ApiResponse<>(data, null);
  }

  public static <T> ApiResponse<List<T>> page(Page<T> page) {
    return new ApiResponse<>(page.getContent(), PageMeta.of(page));
  }
}
