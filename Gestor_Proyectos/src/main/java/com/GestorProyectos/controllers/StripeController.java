package com.GestorProyectos.controllers;

import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.GestorProyectos.dto.ApiResponse;
import com.GestorProyectos.entity.User;
import com.GestorProyectos.repository.UserRepository;
import com.GestorProyectos.service.StripeService;

@RestController
@RequestMapping("/api/stripe")
public class StripeController {

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Autowired private StripeService  stripeService;
    @Autowired private UserRepository userRepo;

    /**
     * POST /api/stripe/checkout
     * Creates a Stripe Checkout session and returns the redirect URL.
     */
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<String>> checkout(Principal principal) {
        try {
            String successUrl = frontendUrl + "/profile?checkout=success";
            String cancelUrl  = frontendUrl + "/profile?checkout=cancel";
            String url = stripeService.createCheckoutSession(
                principal.getName(), successUrl, cancelUrl);
            return ResponseEntity.ok(ApiResponse.ok(url));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to create checkout session: " + e.getMessage()));
        }
    }

    /**
     * POST /api/stripe/portal
     * Opens the Stripe Billing Portal for subscription management.
     */
    @PostMapping("/portal")
    public ResponseEntity<ApiResponse<String>> portal(Principal principal) {
        User user = userRepo.findByName(principal.getName());
        if (user == null || user.getStripeCustomerId() == null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("No active subscription found"));
        }
        try {
            String returnUrl = frontendUrl + "/profile";
            String url = stripeService.createBillingPortalSession(
                user.getStripeCustomerId(), returnUrl);
            return ResponseEntity.ok(ApiResponse.ok(url));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("Failed to open billing portal: " + e.getMessage()));
        }
    }

    /**
     * POST /api/stripe/webhook
     * Stripe webhook — no JWT auth, signature verified inside StripeService.
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            stripeService.handleWebhook(payload, sigHeader);
            return ResponseEntity.ok("ok");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Webhook error");
        }
    }
}
