package com.GestorProyectos.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Table(name = "user")
public class User implements Serializable{

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false)
	private String name;

	@Column(unique = true, nullable = false)
	private String email;

	private String passwordHash;

	@ElementCollection(fetch = FetchType.EAGER)
	private List<String> roles;

	// ── Subscription ──────────────────────────────────────────────────────

	@Column(nullable = false)
	private String plan = "free";

	@Column(name = "stripe_customer_id")
	private String stripeCustomerId;

	@Column(name = "stripe_subscription_id")
	private String stripeSubscriptionId;

	// ── Search quota (rolling monthly, free plan only) ─────────────────────

	@Column(name = "search_count", nullable = false)
	private int searchCount = 0;

	@Column(name = "search_reset_at")
	private LocalDate searchResetAt;
	
	
	public Long getId() { return id; }

	public User() {
	}

	public User(String name, String encodedPassword, String email, String... roles) {
		this.name = name;
		this.email = email;
		this.passwordHash = encodedPassword;
		this.roles = new ArrayList<>(Arrays.asList(roles));
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public List<String> getRoles() {
		return roles;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}

	public String getPlan() { return plan; }
	public void setPlan(String plan) { this.plan = plan; }

	public String getStripeCustomerId() { return stripeCustomerId; }
	public void setStripeCustomerId(String id) { this.stripeCustomerId = id; }

	public String getStripeSubscriptionId() { return stripeSubscriptionId; }
	public void setStripeSubscriptionId(String id) { this.stripeSubscriptionId = id; }

	public int getSearchCount() { return searchCount; }
	public void setSearchCount(int count) { this.searchCount = count; }

	public LocalDate getSearchResetAt() { return searchResetAt; }
	public void setSearchResetAt(LocalDate date) { this.searchResetAt = date; }

}