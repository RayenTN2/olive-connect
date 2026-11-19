package ISIMS.olive_connect.dto;

import ISIMS.olive_connect.entity.ProblemStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateProblemStatusRequest {

    @NotNull(message = "Status is required")
    private ProblemStatus status;

    public ProblemStatus getStatus() { return status; }
    public void setStatus(ProblemStatus status) { this.status = status; }
}