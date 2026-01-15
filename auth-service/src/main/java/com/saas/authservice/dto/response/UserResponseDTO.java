package com.saas.authservice.dto.response;

import java.util.UUID;



public class UserResponseDTO {

    private UUID id;
    private String username;
    private String cellular;
    private String email;

    public UserResponseDTO(UUID id,
                           String username,
                           String cellular,
                           String email) {
        this.id = id;
        this.username = username;
        this.cellular = cellular;
        this.email = email;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCellular() {
        return cellular;
    }

    public void setCellular(String cellular) {
        this.cellular = cellular;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
