package com.example.demo;
import java.util.Properties;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sun.mail.smtp.SMTPTransport;



@RestController
public class ServerController {


    private final String usrname = "gestorproyectosurjc@gmail.com";
	@PostMapping(value="/mail/")
	@ResponseStatus(HttpStatus.CREATED)
	public ResponseEntity<Boolean> sendMail(@RequestBody Email mail) {
		System.out.println("Message received from web : " + mail);
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
			
			// String que portará el mensaje a enviar
			final MimeMessage msg = new MimeMessage(session);

			// -- Set the FROM and TO fields --
			// emisor
			msg.setFrom(new InternetAddress(usrname));
			// receptor
			msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mail.getUserMail(), false));
			// mensaje del correo
			msg.setSubject("Welcome to GestorProyectos!");
			msg.setText(
					"Hi " + mail.getUserName()
							+ "\n\n Thank you for colaborate on our web page,this is your verifing code:"+mail.getVerifycode()
							+ "\n\n We hope you'll enjoy it as much as we enjoyed developing it.","utf-8");
			
			SMTPTransport t = (SMTPTransport) session.getTransport("smtps");
			// se inicia sesión en el correo
			t.connect("smtp.gmail.com", usrname, "movimientoNaranja");
			// se añade el mensaje a enviar
			t.sendMessage(msg, msg.getAllRecipients());
			// se cierra conexión
			t.close();
			System.out.println("correo enviado con exito");
		} catch (MessagingException ex) {
			System.out.println(ex);
		}
		return new ResponseEntity <Boolean> (true, HttpStatus.OK);
	}

}
