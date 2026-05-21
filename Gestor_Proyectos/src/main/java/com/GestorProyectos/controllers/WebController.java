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
import com.GestorProyectos.entity.Consulta;
import com.GestorProyectos.service.SearchService;

@RestController
@RequestMapping("/api")
public class WebController {

    @Autowired
    private SearchService searchService;

    /**
     * Search across platforms.
     * @param platform 1=GitHub, 2=GitLab, 3=StackOverflow, 4=Bitbucket
     * @param query    search keyword
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Consulta>>> search(
            @RequestParam int platform,
            @RequestParam String query) {

        List<Consulta> results = searchService.search(platform, query);
        List<Consulta> page = results.size() > 10 ? results.subList(0, 10) : results;
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    /**
     * Export search results as a plain-text file.
     * Reuses the same cache key as /search so no extra API call is needed.
     */
    @GetMapping("/export")
    public void export(
            @RequestParam int platform,
            @RequestParam String query,
            HttpServletResponse response) {

        List<Consulta> consultas = searchService.getCachedResults(platform + query);
        if (consultas.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        StringBuilder text = new StringBuilder();
        text.append("Id   |    Titulo   |    Autor   |    Numero de visitante\r\n");
        for (Consulta c : consultas) {
            text.append(c.getIdConsulta()).append("   |    ")
                .append(c.getNombre()).append("   |    ")
                .append(c.getAutor()).append("   |    ")
                .append(c.getNumeroVisitante()).append("\r\n");
        }
        exportTxt(response, text.toString());
    }

    private void exportTxt(HttpServletResponse response, String text) {
        response.setCharacterEncoding("utf-8");
        response.setContentType("text/plain");
        response.addHeader("Content-Disposition",
            "attachment;filename=" + genAttachmentFileName("Lista_Consulta", "consulta") + ".txt");
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

    private String genAttachmentFileName(String fileName, String defaultName) {
        try {
            return new String(fileName.getBytes("gb2312"), "ISO8859-1");
        } catch (Exception e) {
            return defaultName;
        }
    }
}
