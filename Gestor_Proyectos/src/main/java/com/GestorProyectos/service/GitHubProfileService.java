package com.GestorProyectos.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.GestorProyectos.Utils.RedisUtils;
import com.GestorProyectos.dto.ContributionStatsDto;
import com.GestorProyectos.dto.DeveloperProfileDto;
import com.GestorProyectos.dto.LanguageStatDto;

@Service
public class GitHubProfileService {

    private static final Logger log = LoggerFactory.getLogger(GitHubProfileService.class);

    private static final String GRAPHQL_URL = "https://api.github.com/graphql";
    private static final long PROFILE_CACHE_SECONDS = 86_400L; // 24 hours
    private static final String CACHE_PREFIX = "profile:github:";

    // Top-100 non-fork public repos by stars; top-10 languages per repo
    private static final String QUERY = """
        query DeveloperProfile($login: String!) {
          user(login: $login) {
            login name bio location company avatarUrl url createdAt email websiteUrl
            followers { totalCount }
            following { totalCount }
            repositories(
              first: 100
              ownerAffiliations: OWNER
              isFork: false
              privacy: PUBLIC
              orderBy: { field: STARGAZERS, direction: DESC }
            ) {
              totalCount
              nodes {
                stargazerCount
                forkCount
                languages(first: 10, orderBy: { field: SIZE, direction: DESC }) {
                  edges { size node { name color } }
                }
              }
            }
            contributionsCollection {
              totalCommitContributions
              totalPullRequestContributions
              totalIssueContributions
              totalPullRequestReviewContributions
              contributionCalendar {
                totalContributions
                weeks {
                  contributionDays { contributionCount date }
                }
              }
            }
          }
        }
        """;

    @Value("${api.github.token}")
    private String githubToken;

    @Autowired
    private RedisUtils redisUtils;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Returns a full developer profile, using Redis to cache results for 24 h.
     * Returns null if the token is not configured or the user is not found.
     */
    public DeveloperProfileDto getProfile(String username) {
        String cacheKey = CACHE_PREFIX + username.toLowerCase();
        if (redisUtils.exists(cacheKey)) {
            return (DeveloperProfileDto) redisUtils.get(cacheKey);
        }
        DeveloperProfileDto profile = fetchFromGitHub(username);
        if (profile != null) {
            redisUtils.set(cacheKey, profile, PROFILE_CACHE_SECONDS);
        }
        return profile;
    }

    /** Evict the cached profile (e.g. on manual refresh request). */
    public void evictCache(String username) {
        redisUtils.remove(CACHE_PREFIX + username.toLowerCase());
    }

    // ─── GraphQL fetch + parse ──────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private DeveloperProfileDto fetchFromGitHub(String username) {
        if (githubToken == null || githubToken.isBlank()) {
            return null;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(githubToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-GitHub-Api-Version", "2022-11-28");

            Map<String, Object> body = Map.of(
                "query", QUERY,
                "variables", Map.of("login", username)
            );

            Map<String, Object> response = restTemplate.exchange(
                GRAPHQL_URL, HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class
            ).getBody();

            if (response == null || response.containsKey("errors")) return null;

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            Map<String, Object> user = (Map<String, Object>) data.get("user");
            if (user == null) return null;

            return parseUser(user);
        } catch (Exception e) {
            log.warn("GitHub GraphQL error for {}: {}", username, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private DeveloperProfileDto parseUser(Map<String, Object> u) {
        String login = (String) u.get("login");

        int followers = nestedInt(u, "followers", "totalCount");
        int following = nestedInt(u, "following", "totalCount");

        // ── Repositories ────────────────────────────────────────────────────
        Map<String, Object> reposMap = (Map<String, Object>) u.get("repositories");
        int totalPublicRepos = toInt(reposMap.get("totalCount"));
        List<Map<String, Object>> repoNodes =
            (List<Map<String, Object>>) reposMap.get("nodes");

        int totalStars = 0;
        int totalForks = 0;
        Map<String, Long> langBytes = new LinkedHashMap<>();
        Map<String, String> langColors = new LinkedHashMap<>();

        for (Map<String, Object> repo : repoNodes) {
            totalStars += toInt(repo.get("stargazerCount"));
            totalForks += toInt(repo.get("forkCount"));

            Map<String, Object> langs = (Map<String, Object>) repo.get("languages");
            if (langs == null) continue;
            List<Map<String, Object>> edges =
                (List<Map<String, Object>>) langs.get("edges");
            if (edges == null) continue;
            for (Map<String, Object> edge : edges) {
                long size = toLong(edge.get("size"));
                Map<String, Object> node = (Map<String, Object>) edge.get("node");
                String name = (String) node.get("name");
                String color = (String) node.get("color");
                langBytes.merge(name, size, Long::sum);
                langColors.putIfAbsent(name, color);
            }
        }

        List<LanguageStatDto> topLanguages = buildLanguageStats(langBytes, langColors);

        // ── Contributions ───────────────────────────────────────────────────
        Map<String, Object> contrib =
            (Map<String, Object>) u.get("contributionsCollection");
        ContributionStatsDto stats = parseContributions(contrib);

        String bio        = (String) u.get("bio");
        String websiteUrl = blankToNull((String) u.get("websiteUrl"));
        String linkedinUrl = extractLinkedIn(bio, websiteUrl);

        return DeveloperProfileDto.builder()
            .id("github:" + login)
            .platform("github")
            .username(login)
            .displayName(stringOrFallback(u, "name", login))
            .avatarUrl((String) u.get("avatarUrl"))
            .profileUrl((String) u.get("url"))
            .bio(bio)
            .location((String) u.get("location"))
            .company(trimAt((String) u.get("company")))
            .joinedAt(isoDateOnly((String) u.get("createdAt")))
            .email(blankToNull((String) u.get("email")))
            .blog(websiteUrl)
            .linkedinUrl(linkedinUrl)
            .followers(followers)
            .following(following)
            .totalPublicRepos(totalPublicRepos)
            .ownedRepos(repoNodes.size())
            .totalStars(totalStars)
            .totalForks(totalForks)
            .topLanguages(topLanguages)
            .contributionStats(stats)
            .build();
    }

    private List<LanguageStatDto> buildLanguageStats(
            Map<String, Long> langBytes, Map<String, String> langColors) {

        long total = langBytes.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) return List.of();

        return langBytes.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .map(e -> new LanguageStatDto(
                e.getKey(),
                langColors.getOrDefault(e.getKey(), "#cccccc"),
                e.getValue(),
                Math.round(e.getValue() * 1000.0 / total) / 10.0
            ))
            .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private ContributionStatsDto parseContributions(Map<String, Object> contrib) {
        if (contrib == null) return new ContributionStatsDto();

        int totalCommits   = toInt(contrib.get("totalCommitContributions"));
        int totalPRs       = toInt(contrib.get("totalPullRequestContributions"));
        int totalIssues    = toInt(contrib.get("totalIssueContributions"));
        int totalReviews   = toInt(contrib.get("totalPullRequestReviewContributions"));

        Map<String, Object> calendar =
            (Map<String, Object>) contrib.get("contributionCalendar");
        int totalContributions = toInt(calendar.get("totalContributions"));

        // Count last 90 and last 30 days from the calendar
        List<Map<String, Object>> weeks =
            (List<Map<String, Object>>) calendar.get("weeks");
        LocalDate cutoff90 = LocalDate.now().minusDays(90);
        LocalDate cutoff30 = LocalDate.now().minusDays(30);
        int last90 = 0, last30 = 0;

        for (Map<String, Object> week : weeks) {
            List<Map<String, Object>> days =
                (List<Map<String, Object>>) week.get("contributionDays");
            for (Map<String, Object> day : days) {
                LocalDate date = LocalDate.parse((String) day.get("date"));
                int count = toInt(day.get("contributionCount"));
                if (!date.isBefore(cutoff90)) last90 += count;
                if (!date.isBefore(cutoff30)) last30 += count;
            }
        }

        return new ContributionStatsDto(
            totalCommits, totalPRs, totalIssues, totalReviews,
            totalContributions, last90, last30
        );
    }

    // ─── LinkedIn extraction ──────────────────────────────────────────────

    private static final Pattern LINKEDIN_PATTERN = Pattern.compile(
        "(?:https?://)?(?:www\\.)?linkedin\\.com/in/([\\w%-]+)/?",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Attempts to extract a LinkedIn profile URL from the developer's bio text
     * or their blog/website field. Returns null if no LinkedIn URL is found.
     */
    static String extractLinkedIn(String bio, String websiteUrl) {
        // Check blog/website field first (direct URL is most reliable)
        if (websiteUrl != null) {
            Matcher m = LINKEDIN_PATTERN.matcher(websiteUrl);
            if (m.find()) return "https://www.linkedin.com/in/" + m.group(1);
        }
        // Fall back to scanning the bio text
        if (bio != null) {
            Matcher m = LINKEDIN_PATTERN.matcher(bio);
            if (m.find()) return "https://www.linkedin.com/in/" + m.group(1);
        }
        return null;
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static int nestedInt(Map<String, Object> map, String key, String inner) {
        Map<String, Object> nested = (Map<String, Object>) map.get(key);
        return nested != null ? toInt(nested.get(inner)) : 0;
    }

    private static int toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Integer i) return i;
        if (val instanceof Number n) return n.intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return 0; }
    }

    private static long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Long l) return l;
        if (val instanceof Number n) return n.longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return 0L; }
    }

    private static String stringOrFallback(Map<String, Object> map, String key, String fallback) {
        String v = (String) map.get(key);
        return (v != null && !v.isBlank()) ? v : fallback;
    }

    private static String trimAt(String s) {
        return s != null ? s.strip().replaceFirst("^@", "") : null;
    }

    private static String blankToNull(String s) {
        return (s != null && !s.isBlank()) ? s : null;
    }

    /** "2012-04-14T02:31:13Z" → "2012-04-14" */
    private static String isoDateOnly(String datetime) {
        return datetime != null && datetime.length() >= 10
            ? datetime.substring(0, 10) : datetime;
    }
}
