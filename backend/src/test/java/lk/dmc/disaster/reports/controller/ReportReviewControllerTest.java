package lk.dmc.disaster.reports.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import lk.dmc.disaster.reports.entity.HazardReport;
import lk.dmc.disaster.reports.entity.RejectionReason;
import lk.dmc.disaster.reports.entity.ReportStatus;
import lk.dmc.disaster.reports.service.QueueItem;
import lk.dmc.disaster.shared.error.BusinessRuleException;
import lk.dmc.disaster.shared.error.InvalidStateTransitionException;
import lk.dmc.disaster.shared.error.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;

class ReportReviewControllerTest extends ReportWebTestSupport {

  private static final UUID OFFICER_ID = UUID.fromString(OFFICER);

  // ---- queue --------------------------------------------------------------------------------

  @Test
  void queue_returnsItemsWithFlagsAndPagingMeta() throws Exception {
    HazardReport report = gpsReport();
    when(queries.queue(null, null, null, 0, 20))
        .thenReturn(
            new PageImpl<>(List.of(new QueueItem(report, true, true)), PageRequest.of(0, 20), 41));

    mvc.perform(get("/api/reports").header(HEADER, OFFICER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(report.getId().toString()))
        .andExpect(jsonPath("$.data[0].hasPhoto").value(true))
        .andExpect(jsonPath("$.data[0].hasDuplicates").value(true))
        .andExpect(jsonPath("$.meta.totalElements").value(41))
        .andExpect(jsonPath("$.meta.totalPages").value(3));
  }

  @Test
  void queue_passesFiltersAndPagingToTheService() throws Exception {
    UUID type = UUID.randomUUID();
    UUID district = UUID.randomUUID();
    when(queries.queue(ReportStatus.PENDING, type, district, 1, 10))
        .thenReturn(new PageImpl<>(List.of()));

    mvc.perform(
            get("/api/reports")
                .param("status", "PENDING")
                .param("hazardTypeId", type.toString())
                .param("districtId", district.toString())
                .param("page", "1")
                .param("size", "10")
                .header(HEADER, OFFICER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isEmpty());
    verify(queries).queue(ReportStatus.PENDING, type, district, 1, 10);
  }

  @Test
  void queue_unknownStatusIs400() throws Exception {
    mvc.perform(get("/api/reports?status=WHATEVER").header(HEADER, OFFICER))
        .andExpect(status().isBadRequest());
  }

  @Test
  void queue_citizenIs403() throws Exception {
    mvc.perform(get("/api/reports").header(HEADER, CITIZEN)).andExpect(status().isForbidden());
    verifyNoInteractions(queries);
  }

  // ---- verify -------------------------------------------------------------------------------

  @Test
  void verify_withoutABodyIs200() throws Exception {
    HazardReport report = gpsReport();
    when(verification.verify(eq(report.getId()), eq(OFFICER_ID), isNull(), isNull()))
        .thenReturn(detailOf(report));

    mvc.perform(patch("/api/reports/" + report.getId() + "/verify").header(HEADER, OFFICER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(report.getId().toString()))
        .andExpect(jsonPath("$.data.reporter.fullName").value("Ruwan Fernando"));
  }

  @Test
  void verify_withACommentPassesItOn() throws Exception {
    HazardReport report = gpsReport();
    when(verification.verify(any(), any(), eq("Confirmed on site"), isNull())).thenReturn(detailOf(report));

    mvc.perform(
            patch("/api/reports/" + report.getId() + "/verify")
                .header(HEADER, OFFICER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"Confirmed on site\"}"))
        .andExpect(status().isOk());
    verify(verification).verify(report.getId(), OFFICER_ID, "Confirmed on site", null);
  }

  @Test
  void verify_withASeverityPassesItOn() throws Exception {
    HazardReport report = gpsReport();
    when(verification.verify(any(), any(), isNull(), eq(4))).thenReturn(detailOf(report));

    mvc.perform(
            patch("/api/reports/" + report.getId() + "/verify")
                .header(HEADER, OFFICER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"severity\":4}"))
        .andExpect(status().isOk());
    verify(verification).verify(report.getId(), OFFICER_ID, null, 4);
  }

  @Test
  void verify_severityOutsideOneToFiveIs400() throws Exception {
    mvc.perform(
            patch("/api/reports/" + UUID.randomUUID() + "/verify")
                .header(HEADER, OFFICER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"severity\":6}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.details.fields.severity").exists());
  }

  @Test
  void verify_commentOverThreeHundredCharactersIs400() throws Exception {
    mvc.perform(
            patch("/api/reports/" + UUID.randomUUID() + "/verify")
                .header(HEADER, OFFICER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"" + "x".repeat(301) + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.details.fields.comment").exists());
  }

  @Test
  void verify_alreadyDecidedIs409WithTheErrorEnvelope() throws Exception {
    when(verification.verify(any(), any(), any(), any()))
        .thenThrow(new InvalidStateTransitionException("VERIFIED", "VERIFIED"));

    mvc.perform(patch("/api/reports/" + UUID.randomUUID() + "/verify").header(HEADER, OFFICER))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"))
        .andExpect(jsonPath("$.error.message").exists());
  }

  @Test
  void verify_ownReportIs422() throws Exception {
    when(verification.verify(any(), any(), any(), any()))
        .thenThrow(new BusinessRuleException("You cannot review your own report."));

    mvc.perform(patch("/api/reports/" + UUID.randomUUID() + "/verify").header(HEADER, OFFICER))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.error.code").value("BUSINESS_RULE"));
  }

  @Test
  void verify_unknownReportIs404() throws Exception {
    when(verification.verify(any(), any(), any(), any())).thenThrow(new NotFoundException("Report not found."));

    mvc.perform(patch("/api/reports/" + UUID.randomUUID() + "/verify").header(HEADER, OFFICER))
        .andExpect(status().isNotFound());
  }

  @Test
  void verify_citizenIs403AndNothingHappens() throws Exception {
    mvc.perform(patch("/api/reports/" + UUID.randomUUID() + "/verify").header(HEADER, CITIZEN))
        .andExpect(status().isForbidden());
    verifyNoInteractions(verification);
  }

  // ---- reject -------------------------------------------------------------------------------

  @Test
  void reject_withReasonAndCommentIs200() throws Exception {
    HazardReport report = gpsReport();
    when(verification.reject(any(), any(), any(), any())).thenReturn(detailOf(report));

    mvc.perform(
            patch("/api/reports/" + report.getId() + "/reject")
                .header(HEADER, OFFICER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"OTHER\",\"comment\":\"Photo is from the internet\"}"))
        .andExpect(status().isOk());
    verify(verification)
        .reject(report.getId(), OFFICER_ID, RejectionReason.OTHER, "Photo is from the internet");
  }

  @Test
  void reject_withoutAReasonIs400() throws Exception {
    mvc.perform(
            patch("/api/reports/" + UUID.randomUUID() + "/reject")
                .header(HEADER, OFFICER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"No reason given\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.details.fields.reason").exists());
    verifyNoInteractions(verification);
  }

  @Test
  void reject_unknownReasonValueIs400() throws Exception {
    mvc.perform(
            patch("/api/reports/" + UUID.randomUUID() + "/reject")
                .header(HEADER, OFFICER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"BORED\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void reject_withoutABodyIs400() throws Exception {
    mvc.perform(patch("/api/reports/" + UUID.randomUUID() + "/reject").header(HEADER, OFFICER))
        .andExpect(status().isBadRequest());
  }

  @Test
  void reject_otherWithoutCommentIs422FromTheDomainRule() throws Exception {
    when(verification.reject(any(), any(), any(), any()))
        .thenThrow(new BusinessRuleException("Explain the rejection when the reason is Other."));

    mvc.perform(
            patch("/api/reports/" + UUID.randomUUID() + "/reject")
                .header(HEADER, OFFICER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"OTHER\"}"))
        .andExpect(status().isUnprocessableContent());
  }

  @Test
  void reject_alreadyVerifiedIs409() throws Exception {
    when(verification.reject(any(), any(), any(), any()))
        .thenThrow(new InvalidStateTransitionException("VERIFIED", "REJECTED"));

    mvc.perform(
            patch("/api/reports/" + UUID.randomUUID() + "/reject")
                .header(HEADER, OFFICER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"DUPLICATE\"}"))
        .andExpect(status().isConflict());
  }

  // ---- request info -------------------------------------------------------------------------

  @Test
  void requestInfo_withACommentIs200() throws Exception {
    HazardReport report = gpsReport();
    when(verification.requestInfo(any(), any(), any())).thenReturn(detailOf(report));

    mvc.perform(
            patch("/api/reports/" + report.getId() + "/request-info")
                .header(HEADER, OFFICER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"Which side of the bridge?\"}"))
        .andExpect(status().isOk());
    verify(verification).requestInfo(report.getId(), OFFICER_ID, "Which side of the bridge?");
  }

  @Test
  void requestInfo_commentShorterThanFiveCharactersIs400() throws Exception {
    mvc.perform(
            patch("/api/reports/" + UUID.randomUUID() + "/request-info")
                .header(HEADER, OFFICER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"why\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.details.fields.comment").exists());
    verifyNoInteractions(verification);
  }

  @Test
  void requestInfo_blankCommentIs400() throws Exception {
    mvc.perform(
            patch("/api/reports/" + UUID.randomUUID() + "/request-info")
                .header(HEADER, OFFICER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"      \"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void requestInfo_notFromPendingIs409() throws Exception {
    when(verification.requestInfo(any(), any(), any()))
        .thenThrow(new InvalidStateTransitionException("NEEDS_MORE_INFO", "NEEDS_MORE_INFO"));

    mvc.perform(
            patch("/api/reports/" + UUID.randomUUID() + "/request-info")
                .header(HEADER, OFFICER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"Which side again?\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("INVALID_STATE_TRANSITION"));
  }

  @Test
  void requestInfo_districtOfficerIs403() throws Exception {
    mvc.perform(
            patch("/api/reports/" + UUID.randomUUID() + "/request-info")
                .header(HEADER, DISTRICT_OFFICER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"Which side again?\"}"))
        .andExpect(status().isForbidden());
  }
}
