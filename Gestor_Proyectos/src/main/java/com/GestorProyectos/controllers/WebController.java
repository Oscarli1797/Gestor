package com.GestorProyectos.controllers;

import java.security.Principal;
import java.util.List;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.GestorProyectos.dto.ApiResponse;
import com.GestorProyectos.dto.DeveloperDto;
import com.GestorProyectos.service.QuotaService;
import com.GestorProyectos.service.QuotaService.QuotaExceededException;
import com.GestorProyectos.service.SearchService;

@RestController
@RequestMapping("/api")
public class WebController {

    private static final Logger log = LoggerFactory.getLogger(WebController.class);

    @Autowired private SearchService searchService;
    @Autowired private QuotaService  quotaService;

    /**
     * Search developers on the given platform.
     * @param platform 1=GitHub 2=GitLab 3=StackOverflow 4=Bitbucket
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<DeveloperDto>>> search(
            @RequestParam int platform,
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "") String location,
            Principal principal) {

        // Only consume quota when the result is not already cached
        if (!searchService.isCached(platform, query, location)) {
            quotaService.checkAndIncrementSearch(principal.getName());
        }
        List<DeveloperDto> results = searchService.search(platform, query, location);
        List<DeveloperDto> page = results.size() > PAGE_SIZE
            ? results.subList(0, PAGE_SIZE) : results;
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleQuota(QuotaExceededException ex) {
        return ResponseEntity.status(429).body(ApiResponse.error(ex.getMessage()));
    }

    /** Export the last search result as a CSV file. */
    @GetMapping("/export")
    public void export(
            @RequestParam int platform,
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "") String location,
            HttpServletResponse response) {

        String cacheKey = platform + ":" + query + ":" + location;
        List<DeveloperDto> developers = searchService.getCachedResults(cacheKey);
        if (developers.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        StringBuilder csv = new StringBuilder();
        csv.append("ID,Platform,Username,Name,Location,Company,Followers,PublicRepos,Email,Website,ProfileUrl\r\n");
        for (DeveloperDto d : developers) {
            csv.append(csvField(d.getId())).append(",")
               .append(csvField(d.getPlatform())).append(",")
               .append(csvField(d.getUsername())).append(",")
               .append(csvField(d.getDisplayName())).append(",")
               .append(csvField(d.getLocation())).append(",")
               .append(csvField(d.getCompany())).append(",")
               .append(d.getFollowers() != null ? d.getFollowers() : 0).append(",")
               .append(d.getPublicRepos() != null ? d.getPublicRepos() : 0).append(",")
               .append(csvField(d.getEmail())).append(",")
               .append(csvField(d.getBlog())).append(",")
               .append(csvField(d.getProfileUrl()))
               .append("\r\n");
        }

        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/csv; charset=UTF-8");
        response.addHeader("Content-Disposition", "attachment; filename=\"developers.csv\"");
        // BOM so Excel opens UTF-8 correctly
        try (ServletOutputStream out = response.getOutputStream()) {
            out.write(0xEF); out.write(0xBB); out.write(0xBF);
            out.write(csv.toString().getBytes("UTF-8"));
            out.flush();
        } catch (Exception e) {
            log.error("Error exporting CSV: {}", e.getMessage());
        }
    }

    private static final int PAGE_SIZE = 10;

    /** Wrap a field in double-quotes and escape internal quotes. Returns empty string for null. */
    private static String csvField(String s) {
        if (s == null || s.isBlank()) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
