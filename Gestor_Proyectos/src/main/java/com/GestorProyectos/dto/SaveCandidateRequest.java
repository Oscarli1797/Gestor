package com.GestorProyectos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SaveCandidateRequest {

    @NotBlank
    private String developerId;   // "github:torvalds"

    @NotBlank
    private String platform;

    @NotBlank
    private String username;

    private String displayName;
    private String avatarUrl;
    private String profileUrl;
    private String location;
    private Integer score;
    private String  scoreTier;
    private String  email;
    private String  blog;
    private String  notes;
}
