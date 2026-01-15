package com.saas.authservice.dto.response;
import java.util.List;
import java.util.UUID;

public class LoginResponseDTO {

    private String accessToken; // <-- El token de corta duración
    private UUID id;
    private String username;
    private String name;
    private String email;
    private List<String> roles;

    // Getters y Setters
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
}

