package lk.dmc.disaster.shared.reference;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.shared.api.ApiResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public reference data. Only districts for now (the sign-up form needs them); river basins, hazard
 * types and the rest are added here by the shared-kernel owners as modules need them.
 */
@RestController
@RequestMapping("/api/reference")
class ReferenceController {

  record DistrictDto(UUID id, String code, String name, String province) {}

  private final JdbcClient jdbc;

  ReferenceController(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @GetMapping("/districts")
  ApiResponse<List<DistrictDto>> districts() {
    List<DistrictDto> districts =
        jdbc.sql("select id, code, name, province from districts order by name")
            .query(DistrictDto.class)
            .list();
    return ApiResponse.of(districts);
  }
}
