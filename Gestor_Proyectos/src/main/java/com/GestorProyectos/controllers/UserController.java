package com.GestorProyectos.controllers;

import java.text.ParseException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import com.GestorProyectos.entity.Email;
import com.GestorProyectos.entity.User;
import com.GestorProyectos.repository.*;

@Controller
public class UserController {
	
	@Autowired
	private UserRepository userRepository;

	@PostMapping(value = "/register")
	public String registroCliente(Model model, HttpSession usuario, HttpServletRequest request,
			@RequestParam String name, @RequestParam String email, @RequestParam String password) {
		usuario.setAttribute("name", name);
		usuario.setAttribute("password", password);
		usuario.setAttribute("email", email);
		usuario.setAttribute("registered", true);

		if ((userRepository.findByName(name) == null) && (name != "") && (email != "")) {

			System.out.println(email);

			User nuevoUsuario = new User(name, password, email, "ROLE_USER");

			userRepository.save(nuevoUsuario);

			boolean aux = !(Boolean) usuario.getAttribute("registered");
			model.addAttribute("unregistered", usuario.getAttribute("registered"));
			model.addAttribute("registered", aux);
			String url = "http://localhost:8070/mail/";
			Email nuevoEmail = new Email(name, email);
			RestTemplate rest = new RestTemplate();
			rest.postForEntity(url, nuevoEmail, String.class);
			System.out.println("Datos enviados!");
			
			model.addAttribute("consulta", false);

			
			return ("Index");

		} else {
			model.addAttribute("alert", "Usuario ya existente");			
			return ("new_user");

		}

	}

	@RequestMapping("/new_user")
	public String new_user(Model model, HttpServletRequest request) {
		model.addAttribute("alert", "");
		// atributos del token		

		return "new_user";
	}

	@RequestMapping("/")
	public String Index(Model model, HttpServletRequest request, HttpSession usuario) throws ParseException {
		if (usuario.getAttribute("registered") == null) {
			usuario.setAttribute("registered", false);

		}
		if (usuario.getAttribute("admin") == null) {
			model.addAttribute("noadmin", true);
		} else {
			model.addAttribute("admin", usuario.getAttribute("admin"));
		}
		model.addAttribute("registered", usuario.getAttribute("registered"));

		boolean aux = !(Boolean) usuario.getAttribute("registered");
		model.addAttribute("unregistered", aux);
		model.addAttribute("consulta", false);


		// model.addAttribute("admin", request.isUserInRole("ADMIN"));
		// model.addAttribute("user", request.isUserInRole("USER"));

		return "Index";
	}

	

	@RequestMapping("/GestorProyectos/Administrador")
	public String Administrador(Model model, HttpServletRequest request, HttpSession usuario) {
		if (usuario.getAttribute("registered") == null) {
			usuario.setAttribute("registered", false);

		}
		model.addAttribute("registered", usuario.getAttribute("registered"));
		boolean aux = !(Boolean) usuario.getAttribute("registered");
		model.addAttribute("unregistered", aux);

		model.addAttribute("admin", request.isUserInRole("ADMIN"));
		model.addAttribute("user", request.isUserInRole("USER"));		

		return "Administrador";
	}

	@GetMapping("/GestorProyectos/Login")
	public String Login(Model model, HttpServletRequest request) {
		model.addAttribute("alert", "");		
		return "Login";
	}

	@RequestMapping("/GestorProyectos/Login/{loged}")
	public String LoginError(Model model, UsernamePasswordAuthenticationToken user, HttpSession usuario,
			@PathVariable String loged, HttpServletRequest request) {
		if (loged.equals("true")) {

			User usur = userRepository.findByName(user.getName());

			usuario.setAttribute("registered", true);
			usuario.setAttribute("name", usur.getName());
			usuario.setAttribute("password", usur.getPasswordHash());
			usuario.setAttribute("email", usur.getEmail());
			if (usur.getRoles().contains("ROLE_ADMIN")) {
				usuario.setAttribute("admin", true);
				model.addAttribute("admin", usuario.getAttribute("admin"));
				boolean aux2 = !(Boolean) usuario.getAttribute("admin");
				model.addAttribute("noadmin", aux2);
			}
			model.addAttribute("registered", usuario.getAttribute("registered"));
			boolean aux = !(Boolean) usuario.getAttribute("registered");
			model.addAttribute("unregistered", aux);	
			model.addAttribute("consulta", false);


			return "Index";
		}
		model.addAttribute("alert", "User or password incorrect");
		

		return "Login";
	}

	@GetMapping("/GestorProyectos/Logout")
	public String Salir(Model model, HttpServletRequest request) {
		model.addAttribute("admin", request.isUserInRole("ADMIN"));
		model.addAttribute("user", request.isUserInRole("USER"));

		

		return "Logout";
	}

	

}