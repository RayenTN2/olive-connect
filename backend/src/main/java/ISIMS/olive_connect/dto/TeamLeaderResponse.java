package ISIMS.olive_connect.dto;

import ISIMS.olive_connect.entity.User;

// Only what planning needs: never the email, phone or CIN of the account
public class TeamLeaderResponse {

    private String id;
    private String fullName;

    public static TeamLeaderResponse fromEntity(User user) {
        TeamLeaderResponse dto = new TeamLeaderResponse();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        return dto;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
}
