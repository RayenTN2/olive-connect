package ISIMS.olive_connect.dto;

import jakarta.validation.constraints.Size;

public class CancelTourRequest {

    @Size(max = 500, message = "Cancellation reason must not exceed 500 characters")
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}