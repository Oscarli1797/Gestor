package com.GestorProyectos.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeveloperDto implements Serializable {

    private String id;           // "github:torvalds"
    private String platform;     // github | gitlab | stackoverflow | bitbucket
    private String username;
    private String displayName;
    private String avatarUrl;
    private String profileUrl;
    private String bio;
    private String location;
    private String company;
    private Integer followers;   // followers on GH/GL, reputation on SO
    private Integer publicRepos;
    private String  email;       // public contact email (GitHub only)
    private String  blog;        // personal website / blog URL (GitHub only)
}
