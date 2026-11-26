package ISIMS.olive_connect.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateTourTeamLeaderRequest {

    @NotBlank(message = "Team leader ID is required")
    private String teamLeaderId;

    public String getTeamLeaderId() { return teamLeaderId; }
    public void setTeamLeaderId(String teamLeaderId) { this.teamLeaderId = teamLeaderId; }
}
