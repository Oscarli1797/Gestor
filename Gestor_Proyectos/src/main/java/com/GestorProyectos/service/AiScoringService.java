package com.GestorProyectos.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.GestorProyectos.Utils.RedisUtils;
import com.GestorProyectos.dto.DeveloperProfileDto;
import com.GestorProyectos.dto.LanguageStatDto;
import com.GestorProyectos.dto.ScoreBreakdownDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Enriches a rule-based score breakdown with a Claude-generated recruiter summary
 * and three key insights.  Results are cached in Redis for 24 h per developer to
 * avoid redundant API calls.
 *
 * Gracefully degrades: if ANTHROPIC_API_KEY is not set, aiSummary / aiInsights
 * are left null and no error is propagated.
 */
@Service
public class AiScoringService {

    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String MODEL = "claude-sonnet-4-6";
    private static final int MAX_TOKENS = 350;
    private static final long CACHE_TTL = 86_400L; // 24 hours
    private static final String CACHE_PREFIX = "ai:profile:";

    @Value("${anthropic.api-key:}")
    private String apiKey;

    @Autowired private RedisUtils redisUtils;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper  objectMapper = new ObjectMapper();

    /**
     * Adds {@code aiSummary} and {@code aiInsights} to the given breakdown.
     * No-op (silent) when the API key is absent or the call fails.
     */
    @SuppressWarnings("unchecked")
    public void enrich(DeveloperProfileDto profile, ScoreBreakdownDto breakdown) {
        if (apiKey == null || apiKey.isBlank()) return;

        String cacheKey = CACHE_PREFIX + profile.getUsername().toLowerCase();
        if (redisUtils.exists(cacheKey)) {
            Map<String, Object> cached = (Map<String, Object>) redisUtils.get(cacheKey);
            breakdown.setAiSummary((String) cached.get("summary"));
            breakdown.setAiInsights((List<String>) cached.get("insights"));
            return;
        }

        try {
            String prompt = buildPrompt(profile, breakdown);
            String responseText = callClaude(prompt);
            if (responseText == null) return;

            JsonNode root = objectMapper.readTree(responseText);
            String summary = root.path("summary").asText(null);
            List<String> insights = null;
            if (root.path("insights").isArray()) {
                insights = objectMapper.convertValue(
                    root.path("insights"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
                );
            }

            breakdown.setAiSummary(summary);
            breakdown.setAiInsights(insights);

            // Cache the result
            redisUtils.set(cacheKey, Map.of(
                "summary",  summary  != null ? summary  : "",
                "insights", insights != null ? insights : List.of()
            ), CACHE_TTL);

        } catch (Exception e) {
            // Silently degrade — AI enrichment is best-effort
            System.out.println("AI scoring skipped for " + profile.getUsername() + ": " + e.getMessage());
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private String buildPrompt(DeveloperProfileDto p, ScoreBreakdownDto sb) {
        String languages = p.getTopLanguages() == null ? "N/A"
            : p.getTopLanguages().stream()
                .limit(5)
                .map(LanguageStatDto::getName)
                .collect(Collectors.joining(", "));

        int last90 = p.getContributionStats() != null
            ? p.getContributionStats().getLast90DaysCommits() : 0;

        return """
            You are helping a technical recruiter evaluate a GitHub developer profile.
            Analyze the profile below and respond ONLY with valid JSON in this exact format (no markdown, no explanation):
            {"summary":"<1-2 sentence recruiter summary>","insights":["<insight 1>","<insight 2>","<insight 3>"]}

            Profile:
            - Name: %s
            - Bio: %s
            - Location: %s
            - Top Languages: %s
            - Stars: %d | Forks: %d | Followers: %d
            - Commits last 90 days: %d
            - Score: %d/100 (%s)
            """.formatted(
                safe(p.getDisplayName()),
                safe(p.getBio()),
                safe(p.getLocation()),
                languages,
                p.getTotalStars(), p.getTotalForks(), p.getFollowers(),
                last90,
                sb.getTotal(), sb.getTier()
            );
    }

    @SuppressWarnings("unchecked")
    private String callClaude(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", ANTHROPIC_VERSION);

        Map<String, Object> body = Map.of(
            "model", MODEL,
            "max_tokens", MAX_TOKENS,
            "messages", List.of(
                Map.of("role", "user", "content", prompt)
            )
        );

        Map<String, Object> response = restTemplate.exchange(
            CLAUDE_API_URL, HttpMethod.POST,
            new HttpEntity<>(body, headers), Map.class
        ).getBody();

        if (response == null) return null;
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
        if (content == null || content.isEmpty()) return null;
        return (String) content.get(0).get("text");
    }

    private static String safe(String s) {
        return s != null ? s : "N/A";
    }
}
