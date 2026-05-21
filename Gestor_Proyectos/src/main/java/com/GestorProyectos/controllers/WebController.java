package com.GestorProyectos.controllers;

import java.io.BufferedOutputStream;
import java.util.List;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.GestorProyectos.dto.ApiResponse;
import com.GestorProyectos.dto.DeveloperDto;
import com.GestorProyectos.service.SearchService;

@RestController
@RequestMapping("/api")
public class WebController {

    @Autowired
    private SearchService searchService;

    /**
     * Search developers on the given platform.
     * @param platform 1=GitHub 2=GitLab 3=StackOverflow 4=Bitbucket
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<DeveloperDto>>> search(
            @RequestParam int platform,
            @RequestParam String query) {

        List<DeveloperDto> results = searchService.search(platform, query);
        List<DeveloperDto> page = results.size() > PAGE_SIZE
            ? results.subList(0, PAGE_SIZE) : results;
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    /** Export the last search result as a plain-text file. */
    @GetMapping("/export")
    public void export(
            @RequestParam int platform,
            @RequestParam String query,
            HttpServletResponse response) {

        String cacheKey = platform + ":" + query;
        List<DeveloperDto> developers = searchService.getCachedResults(cacheKey);
        if (developers.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        StringBuilder text = new StringBuilder();
        text.append("ID   |   Username   |   Name   |   Location   |   Followers\r\n");
        for (DeveloperDto d : developers) {
            text.append(d.getId()).append("   |   ")
                .append(d.getUsername()).append("   |   ")
                .append(nullSafe(d.getDisplayName())).append("   |   ")
                .append(nullSafe(d.getLocation())).append("   |   ")
                .append(d.getFollowers() != null ? d.getFollowers() : 0)
                .append("\r\n");
        }
        exportTxt(response, text.toString());
    }

    private static final int PAGE_SIZE = 10;

    private static String nullSafe(String s) {
        return s != null ? s : "";
    }

    private void exportTxt(HttpServletResponse response, String text) {
        response.setCharacterEncoding("utf-8");
        response.setContentType("text/plain");
        response.addHeader("Content-Disposition",
            "attachment;filename=" + genFileName("Developer_List", "developers") + ".txt");
        BufferedOutputStream buff = null;
        ServletOutputStream outStr = null;
        try {
            outStr = response.getOutputStream();
            buff = new BufferedOutputStream(outStr);
            buff.write(text.getBytes("UTF-8"));
            buff.flush();
        } catch (Exception e) {
            System.out.println("Error exporting file: " + e.getMessage());
        } finally {
            try {
                if (buff != null) buff.close();
                if (outStr != null) outStr.close();
            } catch (Exception ignored) {}
        }
    }

    private String genFileName(String fileName, String defaultName) {
        try {
            return new String(fileName.getBytes("gb2312"), "ISO8859-1");
        } catch (Exception e) {
            return defaultName;
        }
    }
}
