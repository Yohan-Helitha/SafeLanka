package lk.dmc.disaster.response.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AssignmentRequest(
    @NotNull(message = "Event ID is required") UUID eventId,
    UUID warningId,
    UUID districtId,
    @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0", message = "Latitude must be >= -90.0")
        @DecimalMax(value = "90.0", message = "Latitude must be <= 90.0")
        Double latitude,
    @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180.0")
        @DecimalMax(value = "180.0", message = "Longitude must be <= 180.0")
        Double longitude,
    @NotBlank(message = "Location description is required")
        @Size(max = 200, message = "Location text cannot exceed 200 characters")
        String locationText,
    @NotBlank(message = "Task description is required")
        @Size(max = 500, message = "Task description cannot exceed 500 characters")
        String task,
    @NotNull(message = "Priority is required")
        @Min(value = 1, message = "Priority must be between 1 (High) and 3 (Low)")
        @Max(value = 3, message = "Priority must be between 1 (High) and 3 (Low)")
        Short priority,
    @NotNull(message = "Estimated people count is required")
        @Min(value = 0, message = "Estimated people cannot be negative")
        Integer peopleEstimated,
    UUID destinationShelterId,
    UUID teamId) {}
