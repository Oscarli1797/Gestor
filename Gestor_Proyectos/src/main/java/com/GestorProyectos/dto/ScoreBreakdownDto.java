package com.GestorProyectos.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Transparent score breakdown — one sub-score per dimension (0-100 each).
 * The weighted total is also 0-100.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreBreakdownDto implements Serializable {

    // ── Sub-scores (0-100) ───────────────────────────────────────────────────
    /** last-90-day commit activity  (weight 25 %) */
    private int recentActivity;

    /** Stars + forks, log scale     (weight 25 %) */
    private int projectImpact;

    /** Annual commits + owned repos (weight 20 %) */
    private int contributions;

    /** PRs + PR reviews + issues    (weight 15 %) */
    private int collaboration;

    /** Number of languages          (weight 15 %) */
    private int techStack;

    // ── Aggregate ────────────────────────────────────────────────────────────
    /** Weighted total (0-100). */
    private int total;

    /**
     * Human-readable tier:
     * 80-100 Expert | 65-79 Senior | 50-64 Mid-level | 35-49 Junior | 0-34 Beginner
     */
    private String tier;
}
