package lk.dmc.disaster.shared.api;

/** Success envelope: {@code { "data": ... }}. */
public record ApiResponse<T>(T data) {

  public static <T> ApiResponse<T> of(T data) {
    return new ApiResponse<>(data);
  }
}
