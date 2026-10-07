package lk.dmc.disaster.response.dto.response;

public record PageResponse<T>(
    java.util.List<T> items, int page, int size, long totalElements, int totalPages) {}
