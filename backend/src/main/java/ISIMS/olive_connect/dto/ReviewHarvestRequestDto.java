package ISIMS.olive_connect.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReviewHarvestRequestDto {

    @NotNull(message = "Decision is required (true to accept, false to reject)")
    private Boolean accepted;

    @Size(max = 500, message = "Comment must not exceed 500 characters")
    private String comment;

    public Boolean getAccepted() { return accepted; }
    public void setAccepted(Boolean accepted) { this.accepted = accepted; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}