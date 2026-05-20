package com.GestorProyectos.security;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.GestorProyectos.entity.User;
import com.GestorProyectos.repository.UserRepository;

// Solo se ejecuta en perfil "dev". En producción los usuarios se crean manualmente.
// IMPORTANTE: cambia estas contraseñas antes de usar en cualquier entorno compartido.
@Component
@Profile("dev")
public class DatabaseUsersLoader {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@PostConstruct
	private void initDatabase() {
		if (userRepository.findByName("user") == null) {
			userRepository.save(new User("user", passwordEncoder.encode("pass"), "dev-user@example.com", "ROLE_USER"));
		}
		if (userRepository.findByName("admin") == null) {
			userRepository.save(new User("admin", passwordEncoder.encode("adminpass"), "dev-admin@example.com", "ROLE_USER", "ROLE_ADMIN"));
		}
	}

}
