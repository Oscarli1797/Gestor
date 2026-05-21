package com.GestorProyectos.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.GestorProyectos.Utils.RedisUtils;
import com.GestorProyectos.dto.DeveloperDto;

@Service
public class SearchService {

    @Value("${api.github.token}")
    private String githubToken;

    @Value("${api.gitlab.token}")
    private String gitlabToken;

    @Value("${api.stackoverflow.key}")
    private String stackoverflowKey;

    @Autowired
    private RedisUtils redisUtils;

    private static final long CACHE_EXPIRE_SECONDS = 1200L;
    private static final int PAGE_SIZE = 10;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Search developers on the given platform, with Redis caching.
     * @param platform 1=GitHub 2=GitLab 3=StackOverflow 4=Bitbucket
     */
    @SuppressWarnings("unchecked")
    public List<DeveloperDto> search(int platform, String query) {
        String cacheKey = platform + ":" + query;
        if (redisUtils.exists(cacheKey)) {
            return (List<DeveloperDto>) redisUtils.get(cacheKey);
        }
        List<DeveloperDto> results = fetchFromPlatform(platform, query);
        redisUtils.set(cacheKey, results, CACHE_EXPIRE_SECONDS);
        return results;
    }

    @SuppressWarnings("unchecked")
    public List<DeveloperDto> getCachedResults(String cacheKey) {
        if (cacheKey != null && redisUtils.exists(cacheKey)) {
            return (List<DeveloperDto>) redisUtils.get(cacheKey);
        }
        return List.of();
    }

    private List<DeveloperDto> fetchFromPlatform(int platform, String query) {
        return switch (platform) {
            case 1 -> searchGithub(query);
            case 2 -> searchGitlab(query);
            case 3 -> searchStackOverflow(query);
            case 4 -> searchBitbucket(query);
            default -> List.of();
        };
    }

    // ─── GitHub ────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<DeveloperDto> searchGithub(String query) {
        List<DeveloperDto> results = new ArrayList<>();
        try {
            HttpHeaders headers = githubHeaders();
            HttpEntity<?> entity = new HttpEntity<>(headers);

            String url = "https://api.github.com/search/users?q="
                + encode(query) + "&sort=followers&per_page=" + PAGE_SIZE;

            ResponseEntity<Map> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null) return results;

            List<Map<String, Object>> items =
                (List<Map<String, Object>>) body.get("items");
            if (items == null) return results;

            for (Map<String, Object> item : items) {
                String login = (String) item.get("login");
                if (login == null) continue;
                DeveloperDto dev = fetchGithubProfile(login, entity);
                if (dev != null) results.add(dev);
            }
        } catch (Exception e) {
            System.out.println("GitHub search error: " + e.getMessage());
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    private DeveloperDto fetchGithubProfile(String login, HttpEntity<?> entity) {
        try {
            ResponseEntity<Map> res = restTemplate.exchange(
                "https://api.github.com/users/" + login,
                HttpMethod.GET, entity, Map.class);
            Map<String, Object> u = res.getBody();
            if (u == null) return null;

            return DeveloperDto.builder()
                .id("github:" + login)
                .platform("github")
                .username(login)
                .displayName(stringOrFallback(u, "name", login))
                .avatarUrl((String) u.get("avatar_url"))
                .profileUrl((String) u.get("html_url"))
                .bio((String) u.get("bio"))
                .location((String) u.get("location"))
                .company(trimAt((String) u.get("company")))
                .followers(toInt(u.get("followers")))
                .publicRepos(toInt(u.get("public_repos")))
                .email(blankToNull((String) u.get("email")))
                .blog(blankToNull((String) u.get("blog")))
                .build();
        } catch (Exception e) {
            return null;
        }
    }

    private HttpHeaders githubHeaders() {
        HttpHeaders h = new HttpHeaders();
        if (githubToken != null && !githubToken.isEmpty()) {
            h.set("Authorization", "Bearer " + githubToken);
        }
        h.set("Accept", "application/vnd.github.v3+json");
        h.set("X-GitHub-Api-Version", "2022-11-28");
        return h;
    }

    // ─── GitLab ────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<DeveloperDto> searchGitlab(String query) {
        List<DeveloperDto> results = new ArrayList<>();
        try {
            HttpHeaders headers = new HttpHeaders();
            if (gitlabToken != null && !gitlabToken.isEmpty()) {
                headers.set("PRIVATE-TOKEN", gitlabToken);
            }
            HttpEntity<?> entity = new HttpEntity<>(headers);

            String url = "https://gitlab.com/api/v4/users?search="
                + encode(query) + "&per_page=" + PAGE_SIZE + "&active=true";

            ResponseEntity<List> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
            List<Map<String, Object>> users = response.getBody();
            if (users == null) return results;

            for (Map<String, Object> u : users) {
                String username = (String) u.get("username");
                if (username == null) continue;
                results.add(DeveloperDto.builder()
                    .id("gitlab:" + username)
                    .platform("gitlab")
                    .username(username)
                    .displayName(stringOrFallback(u, "name", username))
                    .avatarUrl((String) u.get("avatar_url"))
                    .profileUrl((String) u.get("web_url"))
                    .bio((String) u.get("bio"))
                    .location((String) u.get("location"))
                    .company((String) u.get("organization"))
                    .followers(toInt(u.get("followers")))
                    .publicRepos(null)
                    .build());
            }
        } catch (Exception e) {
            System.out.println("GitLab search error: " + e.getMessage());
        }
        return results;
    }

    // ─── StackOverflow ─────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<DeveloperDto> searchStackOverflow(String query) {
        List<DeveloperDto> results = new ArrayList<>();
        try {
            String url = "https://api.stackexchange.com/2.3/users"
                + "?order=desc&sort=reputation"
                + "&inname=" + encode(query)
                + "&site=stackoverflow"
                + "&pagesize=" + PAGE_SIZE
                + (stackoverflowKey != null && !stackoverflowKey.isEmpty()
                    ? "&key=" + stackoverflowKey : "");

            ResponseEntity<Map> response =
                restTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null) return results;

            List<Map<String, Object>> items =
                (List<Map<String, Object>>) body.get("items");
            if (items == null) return results;

            for (Map<String, Object> u : items) {
                String displayName = (String) u.get("display_name");
                String link = (String) u.get("link");
                String userId = String.valueOf(u.get("user_id"));
                if (displayName == null) continue;
                results.add(DeveloperDto.builder()
                    .id("stackoverflow:" + userId)
                    .platform("stackoverflow")
                    .username(displayName)
                    .displayName(displayName)
                    .avatarUrl((String) u.get("profile_image"))
                    .profileUrl(link)
                    .bio(null)
                    .location((String) u.get("location"))
                    .company(null)
                    .followers(toInt(u.get("reputation")))
                    .publicRepos(toInt(u.get("answer_count")))
                    .build());
            }
        } catch (Exception e) {
            System.out.println("StackOverflow search error: " + e.getMessage());
        }
        return results;
    }

    // ─── Bitbucket ─────────────────────────────────────────────────────────────
    // Bitbucket has no public user search. We search repos and extract unique owners.

    @SuppressWarnings("unchecked")
    private List<DeveloperDto> searchBitbucket(String query) {
        List<DeveloperDto> results = new ArrayList<>();
        try {
            String url = "https://api.bitbucket.org/2.0/repositories?q=name~%22"
                + encode(query) + "%22&pagelen=" + PAGE_SIZE;

            ResponseEntity<Map> response =
                restTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null) return results;

            List<Map<String, Object>> repos =
                (List<Map<String, Object>>) body.get("values");
            if (repos == null) return results;

            for (Map<String, Object> repo : repos) {
                Map<String, Object> workspace =
                    (Map<String, Object>) repo.get("workspace");
                if (workspace == null) continue;

                String slug = (String) workspace.get("slug");
                String name = (String) workspace.getOrDefault("name", slug);
                if (slug == null) continue;

                // Deduplicate
                String id = "bitbucket:" + slug;
                if (results.stream().anyMatch(d -> d.getId().equals(id))) continue;

                Map<String, Object> links = (Map<String, Object>) workspace.get("links");
                String profileUrl = extractHref(links, "html");
                String avatarUrl = extractHref(links, "avatar");

                results.add(DeveloperDto.builder()
                    .id(id)
                    .platform("bitbucket")
                    .username(slug)
                    .displayName(name)
                    .avatarUrl(avatarUrl)
                    .profileUrl(profileUrl)
                    .bio(null)
                    .location(null)
                    .company(null)
                    .followers(null)
                    .publicRepos(null)
                    .build());
            }
        } catch (Exception e) {
            System.out.println("Bitbucket search error: " + e.getMessage());
        }
        return results;
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static Integer toInt(Object val) {
        if (val == null) return null;
        if (val instanceof Integer i) return i;
        if (val instanceof Number n) return n.intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return null; }
    }

    private static String stringOrFallback(Map<String, Object> map, String key, String fallback) {
        String v = (String) map.get(key);
        return (v != null && !v.isBlank()) ? v : fallback;
    }

    /** Remove leading '@' from GitHub company names. */
    private static String trimAt(String s) {
        return s != null ? s.stripLeading().replaceFirst("^@", "") : null;
    }

    private static String blankToNull(String s) {
        return (s != null && !s.isBlank()) ? s : null;
    }

    @SuppressWarnings("unchecked")
    private static String extractHref(Map<String, Object> links, String key) {
        if (links == null) return null;
        Map<String, Object> entry = (Map<String, Object>) links.get(key);
        return entry != null ? (String) entry.get("href") : null;
    }
}
