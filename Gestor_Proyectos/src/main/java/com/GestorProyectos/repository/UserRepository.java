package com.GestorProyectos.repository;

import org.springframework.data.repository.CrudRepository;

import com.GestorProyectos.entity.User;

public interface UserRepository extends CrudRepository<User, Long> {

	User findByName(String name);
    User findByEmail(String email);
}
