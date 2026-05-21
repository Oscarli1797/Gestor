package com.GestorProyectos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.GestorProyectos.entity.Developer;

public interface DeveloperRepository extends JpaRepository<Developer, String> {

    List<Developer> findByPlatform(String platform);
}
