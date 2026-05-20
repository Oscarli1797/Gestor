package com.GestorProyectos.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {

	@Autowired
	public UserRepositoryAuthenticationProvider authenticationProvider;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		http.csrf(csrf -> csrf.disable());

		// Public pages
//		http.authorizeHttpRequests(auth -> auth
//			.requestMatchers("/GestorProyectos").permitAll()
//			.requestMatchers("/GestorProyectos/Login").permitAll()
//			.requestMatchers("/new_user").permitAll()
//			.requestMatchers("/register").permitAll()
//			.requestMatchers("/GestorProyectos/loginError").permitAll()
//			.requestMatchers("/GestorProyectos/Administrador").hasAnyRole("ADMIN")
//		);

		// Login form
		http.formLogin(form -> form
			.loginPage("/GestorProyectos/Login")
			.usernameParameter("username")
			.passwordParameter("password")
			// en caso del correcto inicio de sesion
			.defaultSuccessUrl("/GestorProyectos/Login/true")
			// en caso del incorrecto inicio de sesion
			.failureUrl("/GestorProyectos/Login/false")
		);

		// Logout
		http.logout(logout -> logout
			.logoutUrl("/GestorProyectos/Logout")
			.logoutSuccessUrl("/")
		);

		return http.build();
	}

	@Bean
	public AuthenticationManager authManager(HttpSecurity http) throws Exception {
		AuthenticationManagerBuilder builder =
			http.getSharedObject(AuthenticationManagerBuilder.class);
		// Database authentication provider
		builder.authenticationProvider(authenticationProvider);
		return builder.build();
	}
}