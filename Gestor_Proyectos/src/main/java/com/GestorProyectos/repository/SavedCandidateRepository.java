package com.GestorProyectos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.GestorProyectos.entity.SavedCandidate;

public interface SavedCandidateRepository extends JpaRepository<SavedCandidate, Long> {

    List<SavedCandidate> findByRecruiterIdOrderBySavedAtDesc(Long recruiterId);

    Optional<SavedCandidate> findByRecruiterIdAndDeveloperId(Long recruiterId, String developerId);

    boolean existsByRecruiterIdAndDeveloperId(Long recruiterId, String developerId);

    void deleteByRecruiterIdAndDeveloperId(Long recruiterId, String developerId);
}
