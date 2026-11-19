package ISIMS.olive_connect.dto;

import ISIMS.olive_connect.entity.HarvestRequestType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class CreateHarvestRequestDto {

    @NotBlank(message = "Orchard ID is required")
    private String orchardId;

    @NotNull(message = "Request type is required (PLANNED or URGENT)")
    private HarvestRequestType requestType;

    @FutureOrPresent(message = "Preferred date cannot be in the past")
    private LocalDate preferredDate;

    @Positive(message = "Estimated quantity must be greater than 0")
    private Double estimatedQuantityKg;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    public String getOrchardId() { return orchardId; }
    public void setOrchardId(String orchardId) { this.orchardId = orchardId; }

    public HarvestRequestType getRequestType() { return requestType; }
    public void setRequestType(HarvestRequestType requestType) { this.requestType = requestType; }

    public LocalDate getPreferredDate() { return preferredDate; }
    public void setPreferredDate(LocalDate preferredDate) { this.preferredDate = preferredDate; }

    public Double getEstimatedQuantityKg() { return estimatedQuantityKg; }
    public void setEstimatedQuantityKg(Double estimatedQuantityKg) { this.estimatedQuantityKg = estimatedQuantityKg; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}