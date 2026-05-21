package com.GestorProyectos.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A developer that a recruiter has saved.
 * Stores a denormalized snapshot so the candidates list never needs
 * to hit external APIs again.
 */
@Entity
@Table(
    name = "saved_candidate",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_recruiter_developer",
        columnNames = {"recruiter_id", "developer_id"}
    )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → User.id (the recruiter who saved this candidate). */
    @Column(name = "recruiter_id", nullable = false)
    private Long recruiterId;

    /** "github:torvalds" — matches Developer.id format. */
    @Column(name = "developer_id", nullable = false)
    private String developerId;

    // ── Snapshot ─────────────────────────────────────────────────────────
    private String platform;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String profileUrl;
    private String location;
    private Integer score;
    private String  scoreTier;

    // ── Contact info (from GitHub public data) ────────────────────────────
    private String email;
    private String blog;

    // ── LinkedIn (linked manually by the recruiter) ───────────────────────
    @Column(name = "linkedin_url")
    private String linkedinUrl;

    // ── Recruiter metadata ────────────────────────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime savedAt;
}
