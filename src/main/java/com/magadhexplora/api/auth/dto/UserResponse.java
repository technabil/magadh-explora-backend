package com.magadhexplora.api.auth.dto;

public class UserResponse {
    private String name;
    private String email;
    private String mobile;
    private String role;

    public UserResponse() {}

    public UserResponse(String name, String email, String mobile, String role) {
        this.name = name;
        this.email = email;
        this.mobile = mobile;
        this.role = role;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
