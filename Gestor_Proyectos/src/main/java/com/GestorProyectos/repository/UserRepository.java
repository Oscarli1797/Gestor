package com.GestorProyectos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.GestorProyectos.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

	User findByName(String name);
    User findByEmail(String email);
    User findByStripeCustomerId(String stripeCustomerId);
}
