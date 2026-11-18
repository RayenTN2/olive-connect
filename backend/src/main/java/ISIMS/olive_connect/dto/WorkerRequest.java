package ISIMS.olive_connect.dto;

import jakarta.validation.constraints.NotBlank;

// US-22: Used for both registering and updating a seasonal worker
public class WorkerRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    private String phone;

    // Getters and Setters
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
