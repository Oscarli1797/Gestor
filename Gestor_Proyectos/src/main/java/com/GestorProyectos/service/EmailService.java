package com.GestorProyectos.service;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.GestorProyectos.entity.Email;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${mail.username}")
    private String mailUsername;

    @Value("${mail.password}")
    private String mailPassword;

    public void sendPasswordResetEmail(String toEmail, String code) {
        try {
            MimeMessage msg = createMessage(toEmail);
            msg.setSubject("Password Reset — DevFinder");
            msg.setText(
                "Your password reset code is: " + code
                    + "\n\nThis code expires in 10 minutes."
                    + "\n\nIf you did not request this, you can ignore this email.", "utf-8");
            Transport.send(msg);
        } catch (MessagingException ex) {
            log.error("Error sending reset email to {}: {}", toEmail, ex.getMessage());
        }
    }

    public void sendVerificationEmail(Email mail) {
        try {
            MimeMessage msg = createMessage(mail.getUserMail());
            msg.setSubject("Welcome to DevFinder!");
            msg.setText(
                "Hi " + mail.getUserName()
                    + "\n\nThank you for joining our platform. Your verification code is: "
                    + mail.getVerifycode()
                    + "\n\nThis code expires in 10 minutes.", "utf-8");
            Transport.send(msg);
        } catch (MessagingException ex) {
            log.error("Error sending verification email to {}: {}", mail.getUserMail(), ex.getMessage());
        }
    }

    private MimeMessage createMessage(String toEmail) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(mailUsername, mailPassword);
            }
        });

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(mailUsername));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
        return msg;
    }
}
