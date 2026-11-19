package ISIMS.olive_connect.dto;

import ISIMS.olive_connect.entity.ProblemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReportProblemRequest {

    @NotBlank(message = "Orchard ID is required")
    private String orchardId;

    @NotNull(message = "Problem type is required")
    private ProblemType problemType;

    @NotBlank(message = "Problem description is required")
    @Size(min = 5, max = 1000, message = "Description must be between 5 and 1000 characters")
    private String description;

    public String getOrchardId() { return orchardId; }
    public void setOrchardId(String orchardId) { this.orchardId = orchardId; }

    public ProblemType getProblemType() { return problemType; }
    public void setProblemType(ProblemType problemType) { this.problemType = problemType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}