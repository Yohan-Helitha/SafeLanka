package lk.dmc.disaster.response.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.response.dto.response.AllocationDto;
import lk.dmc.disaster.response.dto.response.DistributionDto;
import lk.dmc.disaster.response.dto.response.ReliefStockDto;
import lk.dmc.disaster.response.entity.AllocationStatus;
import lk.dmc.disaster.response.service.ReliefSuppliesService;
import lk.dmc.disaster.shared.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(MockitoExtension.class)
class ReliefSuppliesControllerTest extends ResponseControllerTestSupport {

  @Mock private ReliefSuppliesService reliefSuppliesService;

  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc = mvcFor(new ReliefSuppliesController(reliefSuppliesService, actingUser));
  }

  @Test
  void listStocks_authorizedRoles_returns200() throws Exception {
    signedInAs(Role.DISTRICT_OFFICER);
    ReliefStockDto stock =
        new ReliefStockDto(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Rice",
            "KG",
            UUID.randomUUID(),
            "DMC",
            DISTRICT,
            1000,
            0L,
            Instant.now(),
            Instant.now());
    when(reliefSuppliesService.getStocks(eq(DISTRICT), any())).thenReturn(List.of(stock));

    mvc.perform(get("/api/relief-stocks").param("districtId", DISTRICT.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].itemName").value("Rice"));

    // Citizen forbidden
    signedInAs(Role.CITIZEN);
    mvc.perform(get("/api/relief-stocks")).andExpect(status().isForbidden());
  }

  @Test
  void allocate_districtOfficer_validRequest_returns201() throws Exception {
    signedInAs(Role.DISTRICT_OFFICER);
    UUID allocId = UUID.randomUUID();
    UUID stockId = UUID.randomUUID();
    UUID shelterId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();

    AllocationDto dto =
        new AllocationDto(
            allocId,
            stockId,
            shelterId,
            eventId,
            50,
            AllocationStatus.ALLOCATED,
            UUID.randomUUID(),
            Instant.now(),
            0L,
            Instant.now(),
            Instant.now());

    when(reliefSuppliesService.allocate(any())).thenReturn(dto);

    String body =
        """
        {
          "stockId": "%s",
          "shelterId": "%s",
          "eventId": "%s",
          "quantity": 50
        }
        """
            .formatted(stockId, shelterId, eventId);

    mvc.perform(post("/api/allocations").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.id").value(allocId.toString()))
        .andExpect(jsonPath("$.data.quantity").value(50));
  }

  @Test
  void allocate_invalidBody_returns400() throws Exception {
    signedInAs(Role.DISTRICT_OFFICER);

    // Quantity must be >= 1
    String body =
        """
        {
          "stockId": "%s",
          "shelterId": "%s",
          "eventId": "%s",
          "quantity": 0
        }
        """
            .formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

    mvc.perform(post("/api/allocations").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
  }

  @Test
  void allocate_unauthorizedRole_returns403() throws Exception {
    signedInAs(Role.SHELTER_COORDINATOR);
    String body =
        """
        {
          "stockId": "%s",
          "shelterId": "%s",
          "eventId": "%s",
          "quantity": 10
        }
        """
            .formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

    mvc.perform(post("/api/allocations").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isForbidden());
  }

  @Test
  void distributions_authorizedRole_returns201() throws Exception {
    signedInAs(Role.SHELTER_COORDINATOR);
    UUID allocId = UUID.randomUUID();
    UUID distId = UUID.randomUUID();

    DistributionDto dto =
        new DistributionDto(
            distId,
            allocId,
            25,
            UUID.randomUUID(),
            Instant.now(),
            false,
            UUID.randomUUID(),
            Instant.now(),
            Instant.now());
    when(reliefSuppliesService.recordDistribution(eq(allocId), any())).thenReturn(dto);

    String body =
        """
        {
          "quantityDistributed": 25,
          "recordedOffline": false
        }
        """;

    mvc.perform(
            post("/api/allocations/{id}/distributions", allocId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.id").value(distId.toString()))
        .andExpect(jsonPath("$.data.quantityDistributed").value(25));
  }

  @Test
  void distributions_invalidQuantity_returns400() throws Exception {
    signedInAs(Role.DISTRICT_OFFICER);
    UUID allocId = UUID.randomUUID();

    String body = "{\"quantityDistributed\": 0}";

    mvc.perform(
            post("/api/allocations/{id}/distributions", allocId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void listAllocations_unpagedAndPaged_returns200() throws Exception {
    signedInAs(Role.DISTRICT_OFFICER);
    UUID allocId = UUID.randomUUID();
    AllocationDto dto =
        new AllocationDto(
            allocId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            30,
            AllocationStatus.ALLOCATED,
            UUID.randomUUID(),
            Instant.now(),
            0L,
            Instant.now(),
            Instant.now());

    when(reliefSuppliesService.listAllocations(any(), any())).thenReturn(List.of(dto));
    when(reliefSuppliesService.getAllocations(any(), any(), eq(0), eq(10)))
        .thenReturn(new PageImpl<>(List.of(dto)));

    // Unpaged
    mvc.perform(get("/api/allocations"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(allocId.toString()));

    // Paged
    mvc.perform(get("/api/allocations").param("page", "0").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(allocId.toString()))
        .andExpect(jsonPath("$.meta.page").value(0))
        .andExpect(jsonPath("$.meta.size").value(1))
        .andExpect(jsonPath("$.meta.totalElements").value(1));

    // Only page param (unpaged fallback)
    mvc.perform(get("/api/allocations").param("page", "0")).andExpect(status().isOk());
    // Only size param (unpaged fallback)
    mvc.perform(get("/api/allocations").param("size", "10")).andExpect(status().isOk());
  }
}
