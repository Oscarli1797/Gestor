package com.GestorProyectos.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableGlobalMethodSecurity(securedEnabled = true)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

	@Autowired
	public UserRepositoryAuthenticationProvider authenticationProvider;

	@Override
	protected void configure(HttpSecurity http) throws Exception {

		http.csrf().disable();
		// Public pages
//		http.authorizeRequests().antMatchers("/GestorProyectos").permitAll();
//
//		// Private pages (all other pages)
//		http.authorizeRequests().antMatchers("/GestorProyectos/Login").permitAll();
//		http.authorizeRequests().antMatchers("/new_user").permitAll();
//		http.authorizeRequests().antMatchers("/register").permitAll();
//		http.authorizeRequests().antMatchers("/GestorProyectos/loginError").permitAll();
//		
//		http.authorizeRequests().antMatchers("/GestorProyectos/Administrador").hasAnyRole("ADMIN");

		
		// Login form
		http.formLogin().loginPage("/GestorProyectos/Login");
		http.formLogin().usernameParameter("username");
		http.formLogin().passwordParameter("password");
		// en caso del correcto inicio de sesion
        http.formLogin().defaultSuccessUrl("/GestorProyectos/Login/true");
        // en caso del incorrecto inicio de sesion
        http.formLogin().failureUrl("/GestorProyectos/Login/false");

		// Logout
		http.logout().logoutUrl("/GestorProyectos/Logout");
		http.logout().logoutSuccessUrl("/");
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		// Database authentication provider
		auth.authenticationProvider(authenticationProvider);
	}
}