package com.GestorProyectos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedCandidateDto {
    private Long   id;
    private String developerId;
    private String platform;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String profileUrl;
    private String location;
    private Integer score;
    private String  scoreTier;
    private String  email;
    private String  blog;
    private String  linkedinUrl;
    private String  notes;
    private String  savedAt;    // ISO-8601 string
}
