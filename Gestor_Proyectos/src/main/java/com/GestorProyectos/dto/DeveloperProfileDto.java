package com.GestorProyectos.dto;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Full developer profile, fetched via GitHub GraphQL. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeveloperProfileDto implements Serializable {

    // ── Identity ────────────────────────────────────────────────────────────
    private String id;           // "github:torvalds"
    private String platform;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String profileUrl;
    private String bio;
    private String location;
    private String company;
    private String joinedAt;     // ISO-8601 date string
    private String email;        // public contact email
    private String blog;         // personal website / blog URL
    private String linkedinUrl;  // extracted from bio/blog (Layer 1) or manually linked

    // ── Engagement ──────────────────────────────────────────────────────────
    private int followers;
    private int following;
    private int totalPublicRepos;
    private int ownedRepos;      // non-fork public repos
    private int totalStars;      // sum of stars across owned repos
    private int totalForks;      // sum of forks across owned repos

    // ── Languages ───────────────────────────────────────────────────────────
    /** Top languages by bytes of code, sorted descending. */
    private List<LanguageStatDto> topLanguages;

    // ── Activity ────────────────────────────────────────────────────────────
    private ContributionStatsDto contributionStats;

    // ── Scoring (populated by ScoringService in DeveloperController) ─────────
    private ScoreBreakdownDto scoreBreakdown;
}
