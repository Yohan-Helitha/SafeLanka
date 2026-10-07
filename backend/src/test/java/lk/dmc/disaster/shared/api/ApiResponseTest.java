package lk.dmc.disaster.shared.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import tools.jackson.databind.json.JsonMapper;

class ApiResponseTest {

  private final JsonMapper json = JsonMapper.builder().build();

  @Test
  void of_wrapsDataWithoutMeta() {
    ApiResponse<String> response = ApiResponse.of("ok");

    assertThat(response.data()).isEqualTo("ok");
    assertThat(response.meta()).isNull();
  }

  @Test
  void of_omitsMetaFromJson() {
    String body = json.writeValueAsString(ApiResponse.of("ok"));

    assertThat(body).isEqualTo("{\"data\":\"ok\"}");
  }

  @Test
  void page_copiesContentAndPagingDetails() {
    var page = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 5);

    ApiResponse<List<String>> response = ApiResponse.page(page);

    assertThat(response.data()).containsExactly("a", "b");
    assertThat(response.meta()).isEqualTo(new PageMeta(1, 2, 5, 3));
  }

  @Test
  void page_emptyPageHasNoContentAndZeroTotals() {
    var page = new PageImpl<String>(List.of(), PageRequest.of(0, 20), 0);

    ApiResponse<List<String>> response = ApiResponse.page(page);

    assertThat(response.data()).isEmpty();
    assertThat(response.meta()).isEqualTo(new PageMeta(0, 20, 0, 0));
  }
}
