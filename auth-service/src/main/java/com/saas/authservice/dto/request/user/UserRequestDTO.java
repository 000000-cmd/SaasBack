package com.saas.authservice.dto.request.user;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public class UserRequestDTO {

    @NotBlank
    private String username;

    @NotBlank
    private String password;
    private String cellular;
    private String email;
    private UUID userAuditId;
    private byte[] attachment;

    // getters y setters manuales
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getCellular() { return cellular; }
    public void setCellular(String cellular) { this.cellular = cellular; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public UUID getUserAuditId() { return userAuditId; }
    public void setUserAuditId(UUID userAuditId) { this.userAuditId = userAuditId; }
    public byte[] getAttachment() {return attachment;}
    public void setAttachment(byte[] attachment) {
        this.attachment = attachment;}
}