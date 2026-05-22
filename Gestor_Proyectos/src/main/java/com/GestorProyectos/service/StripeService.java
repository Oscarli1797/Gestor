package com.GestorProyectos.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.GestorProyectos.entity.User;
import com.GestorProyectos.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.model.billingportal.Session;
import com.stripe.model.checkout.Session.LineItem;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.billingportal.SessionCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

import jakarta.annotation.PostConstruct;

@Service
public class StripeService {

    @Value("${stripe.secret-key:}")
    private String secretKey;

    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;

    @Value("${stripe.pro-price-id:}")
    private String proPriceId;

    @Autowired
    private UserRepository userRepo;

    @PostConstruct
    public void init() {
        if (secretKey != null && !secretKey.isBlank()) {
            Stripe.apiKey = secretKey;
        }
    }

    /**
     * Creates a Stripe Checkout session for upgrading to Pro.
     * If the user doesn't have a Stripe customer yet, one is created.
     *
     * @return the Checkout session URL to redirect the browser to
     */
    @Transactional
    public String createCheckoutSession(String username, String successUrl, String cancelUrl)
            throws Exception {
        User user = requireUser(username);

        // Ensure Stripe customer exists
        if (user.getStripeCustomerId() == null || user.getStripeCustomerId().isBlank()) {
            Customer customer = Customer.create(
                CustomerCreateParams.builder()
                    .setEmail(user.getEmail())
                    .setName(user.getName())
                    .build()
            );
            user.setStripeCustomerId(customer.getId());
            userRepo.save(user);
        }

        com.stripe.model.checkout.Session session =
            com.stripe.model.checkout.Session.create(
                SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(user.getStripeCustomerId())
                    .addLineItem(
                        SessionCreateParams.LineItem.builder()
                            .setPrice(proPriceId)
                            .setQuantity(1L)
                            .build()
                    )
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .build()
            );

        return session.getUrl();
    }

    /**
     * Creates a Stripe Billing Portal session so the user can manage their subscription.
     *
     * @return the portal URL
     */
    public String createBillingPortalSession(String stripeCustomerId, String returnUrl)
            throws Exception {
        Session session = Session.create(
            SessionCreateParams.builder()
                .setCustomer(stripeCustomerId)
                .setReturnUrl(returnUrl)
                .build()
        );
        return session.getUrl();
    }

    /**
     * Validates and processes a Stripe webhook event.
     */
    @Transactional
    public void handleWebhook(String payload, String sigHeader) throws Exception {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new IllegalArgumentException("Invalid Stripe signature");
        }

        switch (event.getType()) {
            case "customer.subscription.created",
                 "customer.subscription.updated" -> {
                Subscription sub = (Subscription) event.getDataObjectDeserializer()
                    .getObject().orElseThrow();
                handleSubscriptionActive(sub);
            }
            case "customer.subscription.deleted" -> {
                Subscription sub = (Subscription) event.getDataObjectDeserializer()
                    .getObject().orElseThrow();
                handleSubscriptionCancelled(sub);
            }
            default -> { /* ignore other events */ }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private void handleSubscriptionActive(Subscription sub) {
        String customerId = sub.getCustomer();
        User user = userRepo.findByStripeCustomerId(customerId);
        if (user == null) return;

        boolean isActive = "active".equals(sub.getStatus())
                        || "trialing".equals(sub.getStatus());
        user.setPlan(isActive ? "pro" : "free");
        user.setStripeSubscriptionId(sub.getId());
        userRepo.save(user);
    }

    private void handleSubscriptionCancelled(Subscription sub) {
        String customerId = sub.getCustomer();
        User user = userRepo.findByStripeCustomerId(customerId);
        if (user == null) return;

        user.setPlan("free");
        user.setStripeSubscriptionId(null);
        userRepo.save(user);
    }

    private User requireUser(String username) {
        User user = userRepo.findByName(username);
        if (user == null) throw new IllegalStateException("User not found: " + username);
        return user;
    }
}
