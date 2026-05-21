package com.GestorProyectos.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Persisted developer profile.
 * Populated when a recruiter saves a candidate (Step 4).
 * Search results and profile fetches use DeveloperDto/DeveloperProfileDto (Redis cache).
 */
@Entity
@Table(name = "developer")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Developer {

    /** Composite key: "github:torvalds", "gitlab:user", etc. */
    @Id
    private String id;

    @Column(nullable = false)
    private String platform;

    @Column(nullable = false)
    private String username;

    private String displayName;
    private String avatarUrl;
    private String profileUrl;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String location;
    private String company;
    private String joinedAt;

    // ── Engagement ──────────────────────────────────────────────────────────
    private Integer followers;
    private Integer following;
    private Integer totalPublicRepos;
    private Integer ownedRepos;
    private Integer totalStars;
    private Integer totalForks;

    // ── Activity (from GitHub GraphQL, populated in Step 2) ─────────────────
    private Integer totalCommitsLastYear;
    @Column(name = "last90_days_activity")
    private Integer last90DaysActivity;

    @Column(name = "last30_days_activity")
    private Integer last30DaysActivity;
    private Integer totalPullRequests;
    private Integer totalIssues;
    private Integer totalPrReviews;

    // ── Languages (JSON: [{name, color, bytes, percentage}]) ────────────────
    @Column(columnDefinition = "TEXT")
    private String topLanguagesJson;

    // ── Scoring (computed by ScoringService, Step 3) ─────────────────────────
    private Integer score;          // weighted total 0-100
    private Integer scoreRecent;    // sub-score: recent activity
    private Integer scoreImpact;    // sub-score: project impact
    private Integer scoreContrib;   // sub-score: contributions
    private Integer scoreCollab;    // sub-score: collaboration
    private Integer scoreTech;      // sub-score: tech stack
    private String  scoreTier;      // Expert / Senior / Mid-level / Junior / Beginner

    private LocalDateTime fetchedAt;
}
