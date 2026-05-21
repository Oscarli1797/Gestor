package com.GestorProyectos.service;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.GestorProyectos.entity.Email;
import com.sun.mail.smtp.SMTPTransport;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Value("${mail.username}")
    private String mailUsername;

    @Value("${mail.password}")
    private String mailPassword;

    public void sendPasswordResetEmail(String toEmail, String code) {
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
                    return new PasswordAuthentication(mailUsername, mailPassword);
                }
            });

            final MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(mailUsername));
            msg.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse(toEmail, false));
            msg.setSubject("Password Reset — DevFinder");
            msg.setText(
                "Your password reset code is: " + code
                    + "\n\nThis code expires in 10 minutes."
                    + "\n\nIf you did not request this, you can ignore this email.", "utf-8");

            SMTPTransport t = (SMTPTransport) session.getTransport("smtps");
            t.connect("smtp.gmail.com", mailUsername, mailPassword);
            t.sendMessage(msg, msg.getAllRecipients());
            t.close();
        } catch (MessagingException ex) {
            System.out.println("Error sending reset email: " + ex.getMessage());
        }
    }

    public void sendVerificationEmail(Email mail) {
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
                    return new PasswordAuthentication(mailUsername, mailPassword);
                }
            });

            final MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(mailUsername));
            msg.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse(mail.getUserMail(), false));
            msg.setSubject("Welcome to GestorProyectos!");
            msg.setText(
                "Hi " + mail.getUserName()
                    + "\n\nThank you for joining our platform. Your verification code is: "
                    + mail.getVerifycode()
                    + "\n\nThis code expires in 10 minutes.", "utf-8");

            SMTPTransport t = (SMTPTransport) session.getTransport("smtps");
            t.connect("smtp.gmail.com", mailUsername, mailPassword);
            t.sendMessage(msg, msg.getAllRecipients());
            t.close();
        } catch (MessagingException ex) {
            System.out.println("Error sending verification email: " + ex.getMessage());
        }
    }
}
