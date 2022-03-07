package com.GestorProyectos.controllers;

import java.text.ParseException;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;


import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;


import com.sun.mail.smtp.SMTPTransport;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.GestorProyectos.Constantes.KConstantes;
import com.GestorProyectos.Utils.RedisUtils;
import com.GestorProyectos.entity.Email;
import com.GestorProyectos.entity.User;
import com.GestorProyectos.repository.*;

@Controller
public class UserController {
	
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private RedisUtils redisUtils;
	//Tiempo de validez para el código de verificación
	private Long redisExpire = (long) (60 * 10000);
	
    private final String usrname = "gestorproyectosurjc@gmail.com";


	@PostMapping(value = "/register")
	public String registroCliente(Model model, HttpSession usuario, HttpServletRequest request,
			@RequestParam String name, @RequestParam String email, @RequestParam String password) {
		usuario.setAttribute("name", name);
		usuario.setAttribute("password", password);
		usuario.setAttribute("email", email);
		usuario.setAttribute("registered", true);

		if ((userRepository.findByName(name) == null) && (userRepository.findByEmail(email) == null) && (name != "") && (email != "")) {

			System.out.println(email);

			User nuevoUsuario = new User(name, password, email, "ROLE_USER");

			//userRepository.save(nuevoUsuario);

			boolean aux = !(Boolean) usuario.getAttribute("registered");
			model.addAttribute("unregistered", usuario.getAttribute("registered"));
			model.addAttribute("registered", aux);
			StringBuffer code=new StringBuffer();
			for(int i=0;i<6;i++) {
				code.append(ThreadLocalRandom.current().nextInt(10));
			}
			Email nuevoEmail = new Email(name, email,code.toString());
			this.sendMail(nuevoEmail);
			System.out.println("Datos enviados!");
			redisUtils.set(KConstantes.RedisConstantes.EMAILCODE + code, code.toString(), redisExpire);
			redisUtils.set(KConstantes.RedisConstantes.NEWUSER + code,nuevoUsuario, redisExpire);

			model.addAttribute("consulta", false);
			model.addAttribute("alert", "");			

			return ("Codeverify");

		} else {
			model.addAttribute("alert", "Usuario ya existente");			
			return ("new_user");

		}

	}
	@PostMapping(value = "/verificar")
	public String Verificar(Model model, HttpServletRequest request, @RequestParam String code) throws Exception {
			try {
			    String redisCode = (String) redisUtils.get(KConstantes.RedisConstantes.EMAILCODE + code);
			    //
			    if (redisCode==null || redisCode.isEmpty()){
					model.addAttribute("alert", "The E-mail verification code is wrong or has expired. Please try again.");			
			    	return ("Codeverify");
			    	}
			    if(redisUtils.get(KConstantes.RedisConstantes.NEWUSER + code).getClass().equals(User.class)) {
				    User nuevoUsuario=(User) redisUtils.get(KConstantes.RedisConstantes.NEWUSER + code);
				    userRepository.save(nuevoUsuario);
				    redisUtils.remove(KConstantes.RedisConstantes.NEWUSER + code);
				    redisUtils.remove(KConstantes.RedisConstantes.EMAILCODE + code);
				    return ("Index");
			    }
			    redisUtils.remove(KConstantes.RedisConstantes.NEWUSER + code);
			    redisUtils.remove(KConstantes.RedisConstantes.EMAILCODE + code);
				model.addAttribute("alert", "No existe dicha Usuario con este código de verificación");			
		    	return ("Codeverify");
			  }catch (Exception ex){
			    throw new Exception(ex.getMessage());
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
	@RequestMapping("/GestorProyectos/profile")
	public String profile(Model model, HttpServletRequest request, HttpSession usuario) {
		if (usuario.getAttribute("registered") == null) {
			usuario.setAttribute("registered", false);

		}
		model.addAttribute("registered", usuario.getAttribute("registered"));
		boolean aux = !(Boolean) usuario.getAttribute("registered");
		model.addAttribute("unregistered", aux);

		model.addAttribute("admin", request.isUserInRole("ADMIN"));
		model.addAttribute("user", request.isUserInRole("USER"));
		model.addAttribute("name",usuario.getAttribute("name"));
		model.addAttribute("email", usuario.getAttribute("email"));

		return "profile";
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
	
	public ResponseEntity<Boolean> sendMail(Email mail) {
		try {
			Properties props = System.getProperties();
			props.setProperty("mail.smtps.host", "smtp.gmail.com");
			props.setProperty("mail.smtp.socketFactory.fallback", "false");
			props.setProperty("mail.smtp.port", "587");
			props.setProperty("mail.smtp.socketFactory.port", "587");
			props.setProperty("mail.smtps.auth", "true");
			props.put("mail.smtps.quitwait", "false");

			Session session = Session.getInstance(props, new Authenticator() {
				@Override
	            protected PasswordAuthentication getPasswordAuthentication() {
	                return new PasswordAuthentication(usrname, "mtiumhzwjbrpkpul");
	            }
			});
			
			final MimeMessage msg = new MimeMessage(session);

			msg.setFrom(new InternetAddress(usrname));
			msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mail.getUserMail(), false));
			msg.setSubject("Welcome to GestorProyectos!");
			msg.setText(
					"Hi " + mail.getUserName()
							+ "\n\n Thank you for colaborate on our web page,this is your verifing code:"+mail.getVerifycode()
							+ "\n\n We hope you'll enjoy it as much as we enjoyed developing it.","utf-8");
			
			SMTPTransport t = (SMTPTransport) session.getTransport("smtps");
			t.connect("smtp.gmail.com", usrname, "movimientoNaranja");
			t.sendMessage(msg, msg.getAllRecipients());
			t.close();
		} catch (MessagingException ex) {
			System.out.println(ex);
		}
		return new ResponseEntity <Boolean> (true, HttpStatus.OK);
	}

	

}