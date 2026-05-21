package com.GestorProyectos.service;

import org.springframework.stereotype.Service;

import com.GestorProyectos.dto.ContributionStatsDto;
import com.GestorProyectos.dto.DeveloperProfileDto;
import com.GestorProyectos.dto.ScoreBreakdownDto;

/**
 * Rule-based developer scoring engine.
 *
 * All sub-scores are 0-100; the final score is a weighted average:
 *   recentActivity 25 % + projectImpact 25 % + contributions 20 %
 *   + collaboration 15 % + techStack 15 %
 *
 * Only GitHub profiles contain enough data for a reliable score.
 */
@Service
public class ScoringService {

    // ── Weights (must sum to 1.0) ─────────────────────────────────────────
    private static final double W_RECENT      = 0.25;
    private static final double W_IMPACT      = 0.25;
    private static final double W_CONTRIB     = 0.20;
    private static final double W_COLLAB      = 0.15;
    private static final double W_TECH        = 0.15;

    public ScoreBreakdownDto score(DeveloperProfileDto profile) {
        int recent  = scoreRecentActivity(profile);
        int impact  = scoreProjectImpact(profile);
        int contrib = scoreContributions(profile);
        int collab  = scoreCollaboration(profile);
        int tech    = scoreTechStack(profile);

        int total = (int) Math.round(
            recent  * W_RECENT  +
            impact  * W_IMPACT  +
            contrib * W_CONTRIB +
            collab  * W_COLLAB  +
            tech    * W_TECH
        );

        return ScoreBreakdownDto.builder()
            .recentActivity(recent)
            .projectImpact(impact)
            .contributions(contrib)
            .collaboration(collab)
            .techStack(tech)
            .total(total)
            .tier(tier(total))
            .build();
    }

    // ── Dimension: Recent Activity ────────────────────────────────────────
    // Based on commits in the last 90 days.
    // 90+ commits (≈1/day) → 100; 45 → 50; 0 → 0.
    // A small boost (+10, capped at 100) is applied when the developer was
    // also active in the last 30 days (sustained activity vs. a burst).
    private int scoreRecentActivity(DeveloperProfileDto p) {
        if (p.getContributionStats() == null) return 0;
        ContributionStatsDto cs = p.getContributionStats();

        int base = cap(cs.getLast90DaysCommits() * 100 / 90);

        // Sustained-activity bonus: last-30-day share > 33 % of last-90
        boolean sustained = cs.getLast90DaysCommits() > 0
            && cs.getLast30DaysCommits() * 3 >= cs.getLast90DaysCommits();
        return cap(base + (sustained ? 10 : 0));
    }

    // ── Dimension: Project Impact ─────────────────────────────────────────
    // Log₁₀ scale on (stars + forks/2).
    //   0 → 0 | 10 stars → ~25 | 100 → ~50 | 1 000 → ~75 | 10 000+ → 100
    private int scoreProjectImpact(DeveloperProfileDto p) {
        double signal = p.getTotalStars() + p.getTotalForks() / 2.0;
        if (signal <= 0) return 0;
        // log10(10001) ≈ 4 → normalise to [0, 100]
        return cap((int) Math.round(Math.log10(signal + 1) / Math.log10(10_001) * 100));
    }

    // ── Dimension: Contributions ──────────────────────────────────────────
    // Commits last year (up to 75 pts) + owned non-fork repos (up to 25 pts).
    private int scoreContributions(DeveloperProfileDto p) {
        if (p.getContributionStats() == null) return 0;
        int commitPart = Math.min(75, p.getContributionStats().getTotalCommits() * 75 / 300);
        int repoPart   = Math.min(25, p.getOwnedRepos() * 25 / 12);
        return cap(commitPart + repoPart);
    }

    // ── Dimension: Collaboration ──────────────────────────────────────────
    // PRs (40 pts max) + PR reviews (40 pts max) + issues (20 pts max).
    // Reviews are weighted highest because they demonstrate active mentorship.
    private int scoreCollaboration(DeveloperProfileDto p) {
        if (p.getContributionStats() == null) return 0;
        ContributionStatsDto cs = p.getContributionStats();
        int prPart     = Math.min(40, cs.getTotalPullRequests() * 40 / 20);
        int reviewPart = Math.min(40, cs.getTotalPrReviews()    * 40 / 10);
        int issuePart  = Math.min(20, cs.getTotalIssues()       * 20 / 20);
        return cap(prPart + reviewPart + issuePart);
    }

    // ── Dimension: Tech Stack ─────────────────────────────────────────────
    // 40 pts base (developer has code) + up to 60 pts for language breadth.
    // 1 language → 50 | 2 → 60 | 6+ → 100
    private int scoreTechStack(DeveloperProfileDto p) {
        if (p.getTopLanguages() == null || p.getTopLanguages().isEmpty()) return 0;
        int langCount = p.getTopLanguages().size();
        return cap(40 + Math.min(60, (langCount - 1) * 12));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static int cap(int v) {
        return Math.max(0, Math.min(100, v));
    }

    private static String tier(int total) {
        if (total >= 80) return "Expert";
        if (total >= 65) return "Senior";
        if (total >= 50) return "Mid-level";
        if (total >= 35) return "Junior";
        return "Beginner";
    }
}
