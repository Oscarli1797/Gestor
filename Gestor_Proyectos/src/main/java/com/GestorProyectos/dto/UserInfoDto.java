package com.GestorProyectos.dto;

import java.util.List;

public class UserInfoDto {
    private String name;
    private String email;
    private List<String> roles;

    public UserInfoDto(String name, String email, List<String> roles) {
        this.name = name;
        this.email = email;
        this.roles = roles;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public List<String> getRoles() { return roles; }
}
