package ISIMS.olive_connect.dto;

import ISIMS.olive_connect.entity.MaturityLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReportMaturityRequest {

    @NotBlank(message = "Orchard ID is required")
    private String orchardId;

    @NotNull(message = "Maturity level is required")
    private MaturityLevel maturityLevel;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    public String getOrchardId() { return orchardId; }
    public void setOrchardId(String orchardId) { this.orchardId = orchardId; }

    public MaturityLevel getMaturityLevel() { return maturityLevel; }
    public void setMaturityLevel(MaturityLevel maturityLevel) { this.maturityLevel = maturityLevel; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}