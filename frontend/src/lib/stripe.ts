import { api } from "./api";

export interface PlanInfo {
  plan: "free" | "pro";
  searchCount: number;
  searchLimit: number;   // -1 = unlimited
  candidateCount: number;
  candidateLimit: number; // -1 = unlimited
}

export async function getPlan(): Promise<PlanInfo> {
  const { data } = await api.get("/api/auth/plan");
  return data.data as PlanInfo;
}

/** Redirects the browser to Stripe Checkout. */
export async function startCheckout(): Promise<void> {
  const { data } = await api.post("/api/stripe/checkout");
  window.location.href = data.data as string;
}

/** Redirects the browser to Stripe Billing Portal. */
export async function openBillingPortal(): Promise<void> {
  const { data } = await api.post("/api/stripe/portal");
  window.location.href = data.data as string;
}
