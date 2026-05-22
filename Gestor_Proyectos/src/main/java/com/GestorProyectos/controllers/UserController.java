package com.GestorProyectos.controllers;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.GestorProyectos.dto.ApiResponse;
import com.GestorProyectos.dto.ForgotPasswordRequest;
import com.GestorProyectos.dto.PlanDto;
import com.GestorProyectos.dto.RegisterRequest;
import com.GestorProyectos.dto.ResetPasswordRequest;
import com.GestorProyectos.dto.UserInfoDto;
import com.GestorProyectos.dto.VerifyRequest;
import com.GestorProyectos.entity.User;
import com.GestorProyectos.repository.SavedCandidateRepository;
import com.GestorProyectos.service.QuotaService;
import com.GestorProyectos.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    @Autowired private UserService              userService;
    @Autowired private QuotaService             quotaService;
    @Autowired private SavedCandidateRepository candidateRepo;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest req) {
        if (!userService.isNameAvailable(req.getName())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Username already taken"));
        }
        if (!userService.isEmailAvailable(req.getEmail())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Email already registered"));
        }
        userService.initiateRegistration(req.getName(), req.getPassword(), req.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("Verification code sent to " + req.getEmail(), null));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verify(@Valid @RequestBody VerifyRequest req) {
        if (userService.verifyAndSave(req.getCode())) {
            return ResponseEntity.ok(ApiResponse.ok("Account verified successfully", null));
        }
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("The verification code is wrong or has expired"));
    }

    /** POST /api/auth/forgot-password — send a reset code to the given email. */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest req) {
        userService.initiatePasswordReset(req.getEmail());
        // Always return 200 to avoid revealing whether the email is registered
        return ResponseEntity.ok(ApiResponse.ok(
            "If that email is registered you will receive a reset code shortly.", null));
    }

    /** POST /api/auth/reset-password — validate the code and set a new password. */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest req) {
        if (userService.resetPassword(req.getCode(), req.getNewPassword())) {
            return ResponseEntity.ok(ApiResponse.ok("Password updated successfully.", null));
        }
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("The reset code is invalid or has expired."));
    }

    /** GET /api/auth/plan — current plan + usage stats. */
    @GetMapping("/plan")
    public ResponseEntity<ApiResponse<PlanDto>> plan(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
        }
        User user = userService.findByName(principal.getName());
        if (user == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("User not found"));
        }
        boolean isPro = "pro".equals(user.getPlan());
        long candidateCount = candidateRepo.countByRecruiterId(user.getId());
        PlanDto dto = PlanDto.builder()
            .plan(user.getPlan())
            .searchCount(quotaService.currentSearchCount(user))
            .searchLimit(isPro ? -1 : QuotaService.FREE_SEARCH_LIMIT)
            .candidateCount(candidateCount)
            .candidateLimit(isPro ? -1 : QuotaService.FREE_CANDIDATE_LIMIT)
            .build();
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoDto>> me(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
        }
        User user = userService.findByName(principal.getName());
        if (user == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("User not found"));
        }
        return ResponseEntity.ok(ApiResponse.ok(
            new UserInfoDto(user.getName(), user.getEmail(), List.copyOf(user.getRoles()))));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }
}
