package com.GestorProyectos.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.GestorProyectos.dto.ApiResponse;
import com.GestorProyectos.dto.DeveloperProfileDto;
import com.GestorProyectos.dto.ScoreBreakdownDto;
import com.GestorProyectos.service.AiScoringService;
import com.GestorProyectos.service.GitHubProfileService;
import com.GestorProyectos.service.ScoringService;

@RestController
@RequestMapping("/api/developer")
public class DeveloperController {

    @Autowired private GitHubProfileService gitHubProfileService;
    @Autowired private ScoringService        scoringService;
    @Autowired private AiScoringService      aiScoringService;

    /**
     * GET /api/developer/github/{username}/profile
     * Returns full profile + score breakdown. Cached 24 h in Redis.
     */
    @GetMapping("/github/{username}/profile")
    public ResponseEntity<ApiResponse<DeveloperProfileDto>> githubProfile(
            @PathVariable String username) {

        DeveloperProfileDto profile = gitHubProfileService.getProfile(username);
        if (profile == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(
                "Profile not found. Ensure GITHUB_TOKEN is configured and the username exists."));
        }

        // Rule-based score (fast, no I/O)
        ScoreBreakdownDto breakdown = scoringService.score(profile);
        // Claude AI enrichment — adds aiSummary + aiInsights (cached 24h, no-op if key absent)
        aiScoringService.enrich(profile, breakdown);
        profile.setScoreBreakdown(breakdown);

        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    /**
     * DELETE /api/developer/github/{username}/profile/cache
     * Evicts the Redis cache entry so the next GET fetches fresh data.
     */
    @DeleteMapping("/github/{username}/profile/cache")
    public ResponseEntity<ApiResponse<Void>> evictCache(@PathVariable String username) {
        gitHubProfileService.evictCache(username);
        return ResponseEntity.ok(ApiResponse.ok("Cache evicted for " + username, null));
    }
}
