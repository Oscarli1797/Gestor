package com.GestorProyectos.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.GestorProyectos.entity.User;
import com.GestorProyectos.service.UserService;

@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping(value = "/register")
    public String registroCliente(Model model, HttpSession session,
                                  @RequestParam String name,
                                  @RequestParam String email,
                                  @RequestParam String password) {
        session.setAttribute("name", name);
        session.setAttribute("email", email);
        session.setAttribute("registered", true);

        if (!userService.isNameAvailable(name) || !userService.isEmailAvailable(email)
                || name.isEmpty() || email.isEmpty()) {
            model.addAttribute("alert", "Usuario ya existente");
            return "new_user";
        }

        userService.initiateRegistration(name, password, email);

        model.addAttribute("unregistered", session.getAttribute("registered"));
        model.addAttribute("registered", !(Boolean) session.getAttribute("registered"));
        model.addAttribute("consulta", false);
        model.addAttribute("alert", "");
        return "Codeverify";
    }

    @PostMapping(value = "/verificar")
    public String verificar(Model model, @RequestParam String code) {
        if (userService.verifyAndSave(code)) {
            return "Index";
        }
        model.addAttribute("alert", "The verification code is wrong or has expired. Please try again.");
        return "Codeverify";
    }

    @RequestMapping("/new_user")
    public String newUser(Model model) {
        model.addAttribute("alert", "");
        return "new_user";
    }

    @RequestMapping("/")
    public String index(Model model, HttpSession session) {
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
        model.addAttribute("consulta", false);
        return "Index";
    }

    @RequestMapping("/GestorProyectos/Administrador")
    public String administrador(Model model, HttpServletRequest request, HttpSession session) {
        if (session.getAttribute("registered") == null) {
            session.setAttribute("registered", false);
        }
        model.addAttribute("registered", session.getAttribute("registered"));
        model.addAttribute("unregistered", !(Boolean) session.getAttribute("registered"));
        model.addAttribute("admin", request.isUserInRole("ADMIN"));
        model.addAttribute("user", request.isUserInRole("USER"));
        return "Administrador";
    }

    @GetMapping("/GestorProyectos/Login")
    public String login(Model model) {
        model.addAttribute("alert", "");
        return "Login";
    }

    @RequestMapping("/GestorProyectos/profile")
    public String profile(Model model, HttpServletRequest request, HttpSession session) {
        if (session.getAttribute("registered") == null) {
            session.setAttribute("registered", false);
        }
        model.addAttribute("registered", session.getAttribute("registered"));
        model.addAttribute("unregistered", !(Boolean) session.getAttribute("registered"));
        model.addAttribute("admin", request.isUserInRole("ADMIN"));
        model.addAttribute("user", request.isUserInRole("USER"));
        model.addAttribute("name", session.getAttribute("name"));
        model.addAttribute("email", session.getAttribute("email"));
        return "profile";
    }

    @RequestMapping("/GestorProyectos/Login/{loged}")
    public String loginResult(Model model, UsernamePasswordAuthenticationToken user,
                              HttpSession session, @PathVariable String loged) {
        if ("true".equals(loged)) {
            User usur = userService.findByName(user.getName());
            session.setAttribute("registered", true);
            session.setAttribute("name", usur.getName());
            session.setAttribute("email", usur.getEmail());
            if (usur.getRoles().contains("ROLE_ADMIN")) {
                session.setAttribute("admin", true);
                model.addAttribute("admin", true);
                model.addAttribute("noadmin", false);
            }
            model.addAttribute("registered", true);
            model.addAttribute("unregistered", false);
            model.addAttribute("consulta", false);
            return "Index";
        }
        model.addAttribute("alert", "User or password incorrect");
        return "Login";
    }

    @GetMapping("/GestorProyectos/Logout")
    public String logout(Model model, HttpServletRequest request) {
        model.addAttribute("admin", request.isUserInRole("ADMIN"));
        model.addAttribute("user", request.isUserInRole("USER"));
        return "Logout";
    }
}
