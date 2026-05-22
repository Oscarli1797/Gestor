package com.GestorProyectos.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.GestorProyectos.dto.SaveCandidateRequest;
import com.GestorProyectos.dto.SavedCandidateDto;
import com.GestorProyectos.entity.SavedCandidate;
import com.GestorProyectos.entity.User;
import com.GestorProyectos.repository.SavedCandidateRepository;
import com.GestorProyectos.repository.UserRepository;
import com.GestorProyectos.service.QuotaService;

@Service
public class CandidateService {

    @Autowired private SavedCandidateRepository candidateRepo;
    @Autowired private UserRepository            userRepository;
    @Autowired private QuotaService              quotaService;

    /** All candidates saved by this recruiter, newest first. */
    public List<SavedCandidateDto> listForUser(String username) {
        long uid = requireUser(username).getId();
        return candidateRepo.findByRecruiterIdOrderBySavedAtDesc(uid)
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    /** Returns true if the recruiter has already saved this developer. */
    public boolean isSaved(String username, String developerId) {
        long uid = requireUser(username).getId();
        return candidateRepo.existsByRecruiterIdAndDeveloperId(uid, developerId);
    }

    /**
     * Save a developer. Idempotent: if already saved, the existing record
     * is returned unchanged (notes are NOT overwritten on re-save).
     */
    public SavedCandidateDto save(String username, SaveCandidateRequest req) {
        User user = requireUser(username);
        // Only check quota for new saves (idempotent re-save is free)
        if (!candidateRepo.existsByRecruiterIdAndDeveloperId(user.getId(), req.getDeveloperId())) {
            quotaService.checkCandidateSave(username);
        }
        return candidateRepo
            .findByRecruiterIdAndDeveloperId(user.getId(), req.getDeveloperId())
            .map(this::toDto)
            .orElseGet(() -> {
                SavedCandidate sc = SavedCandidate.builder()
                    .recruiterId(user.getId())
                    .developerId(req.getDeveloperId())
                    .platform(req.getPlatform())
                    .username(req.getUsername())
                    .displayName(req.getDisplayName())
                    .avatarUrl(req.getAvatarUrl())
                    .profileUrl(req.getProfileUrl())
                    .location(req.getLocation())
                    .score(req.getScore())
                    .scoreTier(req.getScoreTier())
                    .status("saved")
                    .email(req.getEmail())
                    .blog(req.getBlog())
                    .notes(req.getNotes())
                    .savedAt(LocalDateTime.now())
                    .build();
                return toDto(candidateRepo.save(sc));
            });
    }

    /** Remove a saved candidate. No-op if not saved. */
    @Transactional
    public void remove(String username, String developerId) {
        long uid = requireUser(username).getId();
        candidateRepo.deleteByRecruiterIdAndDeveloperId(uid, developerId);
    }

    /** Update the recruiter's private notes for a saved candidate. */
    public SavedCandidateDto updateNotes(String username, String developerId, String notes) {
        long uid = requireUser(username).getId();
        SavedCandidate sc = candidateRepo
            .findByRecruiterIdAndDeveloperId(uid, developerId)
            .orElseThrow(() -> new IllegalArgumentException("Candidate not saved"));
        sc.setNotes(notes);
        return toDto(candidateRepo.save(sc));
    }

    private static final java.util.Set<String> VALID_STATUSES = java.util.Set.of(
        "saved", "contacted", "replied", "interviewing", "offered", "rejected"
    );

    /** Update the pipeline status for a saved candidate. */
    public SavedCandidateDto updateStatus(String username, String developerId, String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
        long uid = requireUser(username).getId();
        SavedCandidate sc = candidateRepo
            .findByRecruiterIdAndDeveloperId(uid, developerId)
            .orElseThrow(() -> new IllegalArgumentException("Candidate not saved"));
        sc.setStatus(status);
        return toDto(candidateRepo.save(sc));
    }

    /** Link (or clear) a LinkedIn profile URL for a saved candidate. */
    public SavedCandidateDto updateLinkedIn(String username, String developerId, String linkedinUrl) {
        long uid = requireUser(username).getId();
        SavedCandidate sc = candidateRepo
            .findByRecruiterIdAndDeveloperId(uid, developerId)
            .orElseThrow(() -> new IllegalArgumentException("Candidate not saved"));
        sc.setLinkedinUrl((linkedinUrl != null && !linkedinUrl.isBlank()) ? linkedinUrl : null);
        return toDto(candidateRepo.save(sc));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private User requireUser(String username) {
        User user = userRepository.findByName(username);
        if (user == null) throw new IllegalStateException("User not found: " + username);
        return user;
    }

    private SavedCandidateDto toDto(SavedCandidate sc) {
        return SavedCandidateDto.builder()
            .id(sc.getId())
            .developerId(sc.getDeveloperId())
            .platform(sc.getPlatform())
            .username(sc.getUsername())
            .displayName(sc.getDisplayName())
            .avatarUrl(sc.getAvatarUrl())
            .profileUrl(sc.getProfileUrl())
            .location(sc.getLocation())
            .score(sc.getScore())
            .scoreTier(sc.getScoreTier())
            .status(sc.getStatus() != null ? sc.getStatus() : "saved")
            .email(sc.getEmail())
            .blog(sc.getBlog())
            .linkedinUrl(sc.getLinkedinUrl())
            .notes(sc.getNotes())
            .savedAt(sc.getSavedAt() != null ? sc.getSavedAt().toString() : null)
            .build();
    }
}
