package com.GestorProyectos.controllers;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.GestorProyectos.dto.ApiResponse;
import com.GestorProyectos.dto.SaveCandidateRequest;
import com.GestorProyectos.dto.SavedCandidateDto;
import com.GestorProyectos.service.CandidateService;

@RestController
@RequestMapping("/api/candidates")
public class CandidateController {

    @Autowired
    private CandidateService candidateService;

    /** GET /api/candidates — list all saved candidates for the current recruiter. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SavedCandidateDto>>> list(Principal principal) {
        return ResponseEntity.ok(
            ApiResponse.ok(candidateService.listForUser(principal.getName())));
    }

    /**
     * GET /api/candidates/saved?developerId=github:torvalds
     * Check whether a specific developer is saved.
     * Using @RequestParam to avoid Spring's colon-in-path-variable limitation.
     */
    @GetMapping("/saved")
    public ResponseEntity<ApiResponse<Boolean>> isSaved(
            @RequestParam String developerId, Principal principal) {
        return ResponseEntity.ok(
            ApiResponse.ok(candidateService.isSaved(principal.getName(), developerId)));
    }

    /** POST /api/candidates — save a developer. Idempotent. */
    @PostMapping
    public ResponseEntity<ApiResponse<SavedCandidateDto>> save(
            @Valid @RequestBody SaveCandidateRequest req, Principal principal) {
        return ResponseEntity.ok(
            ApiResponse.ok(candidateService.save(principal.getName(), req)));
    }

    /**
     * DELETE /api/candidates?developerId=github:torvalds — unsave a developer.
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> remove(
            @RequestParam String developerId, Principal principal) {
        candidateService.remove(principal.getName(), developerId);
        return ResponseEntity.ok(ApiResponse.ok("Candidate removed", null));
    }

    /**
     * PATCH /api/candidates/notes?developerId=github:torvalds
     * Update recruiter's private notes for a saved candidate.
     */
    @PatchMapping("/notes")
    public ResponseEntity<ApiResponse<SavedCandidateDto>> updateNotes(
            @RequestParam String developerId,
            @RequestBody Map<String, String> body,
            Principal principal) {
        String notes = body.getOrDefault("notes", "");
        return ResponseEntity.ok(
            ApiResponse.ok(candidateService.updateNotes(
                principal.getName(), developerId, notes)));
    }

    /**
     * PATCH /api/candidates/status?developerId=github:torvalds
     * Update the pipeline status for a saved candidate.
     * Valid values: saved | contacted | replied | interviewing | offered | rejected
     */
    @PatchMapping("/status")
    public ResponseEntity<ApiResponse<SavedCandidateDto>> updateStatus(
            @RequestParam String developerId,
            @RequestBody Map<String, String> body,
            Principal principal) {
        String status = body.getOrDefault("status", "saved");
        try {
            return ResponseEntity.ok(
                ApiResponse.ok(candidateService.updateStatus(
                    principal.getName(), developerId, status)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * PATCH /api/candidates/linkedin?developerId=github:torvalds
     * Link or clear a LinkedIn profile URL for a saved candidate.
     */
    @PatchMapping("/linkedin")
    public ResponseEntity<ApiResponse<SavedCandidateDto>> updateLinkedIn(
            @RequestParam String developerId,
            @RequestBody Map<String, String> body,
            Principal principal) {
        String linkedinUrl = body.getOrDefault("linkedinUrl", "");
        return ResponseEntity.ok(
            ApiResponse.ok(candidateService.updateLinkedIn(
                principal.getName(), developerId, linkedinUrl)));
    }
}
