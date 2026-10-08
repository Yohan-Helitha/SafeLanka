package lk.dmc.disaster.shared.reference;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lk.dmc.disaster.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The frontend builds the "which districts does this river basin reach" lists from {@code
 * districtIds}; a basin without it breaks the New warning screen.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ReferenceRiverBasinsEndpointTest {

  @Autowired MockMvc mvc;

  @Test
  void everyRiverBasinCarriesItsDistrictIds() throws Exception {
    mvc.perform(get("/api/reference/river-basins"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").exists())
        .andExpect(jsonPath("$[0].districtIds").isArray())
        .andExpect(jsonPath("$[0].districtIds[0]").exists())
        .andExpect(jsonPath("$[1].districtIds").isArray());
  }
}
