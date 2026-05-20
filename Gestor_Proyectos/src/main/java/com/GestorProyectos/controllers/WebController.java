package com.GestorProyectos.controllers;

import java.io.BufferedOutputStream;
import java.util.List;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.GestorProyectos.entity.Consulta;
import com.GestorProyectos.service.SearchService;

@Controller
public class WebController {

    @Autowired
    private SearchService searchService;

    @RequestMapping(value = "/buscador")
    public String buscador(Model model, HttpSession session, HttpServletResponse response,
                           @RequestParam int valor, @RequestParam String nombre) {
        setupSessionModel(model, session);

        List<Consulta> consultas = searchService.search(valor, nombre);
        model.addAttribute("lista", consultas.size() > 10 ? consultas.subList(0, 10) : consultas);
        model.addAttribute("consulta", true);
        session.setAttribute("clave", valor + nombre);
        return "Index";
    }

    @RequestMapping(value = "/search")
    public String searchPage(Model model, HttpServletRequest request, HttpSession session) {
        setupSessionModel(model, session);
        return "search";
    }

    @RequestMapping("/exporttext")
    public void exportConsulta(HttpServletResponse response, HttpSession session) {
        String clave = (String) session.getAttribute("clave");
        List<Consulta> consultas = searchService.getCachedResults(clave);
        if (consultas.isEmpty()) return;

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

    private void setupSessionModel(Model model, HttpSession session) {
        if (session.getAttribute("registered") == null) {
            session.setAttribute("registered", false);
        }
        if (session.getAttribute("admin") == null) {
            model.addAttribute("noadmin", true);
        } else {
            model.addAttribute("admin", session.getAttribute("admin"));
        }
        model.addAttribute("registered", session.getAttribute("registered"));
        model.addAttribute("unregistered", !(Boolean) session.getAttribute("registered"));
    }
}
