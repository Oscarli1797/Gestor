"use client";

import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { getMe, UserInfo } from "@/lib/auth";
import { getPlan, startCheckout, openBillingPortal, PlanInfo } from "@/lib/stripe";

export default function ProfilePage() {
  const [user, setUser]       = useState<UserInfo | null>(null);
  const [plan, setPlan]       = useState<PlanInfo | null>(null);
  const [error, setError]     = useState("");
  const [loading, setLoading] = useState(false);
  const searchParams          = useSearchParams();
  const checkoutResult        = searchParams.get("checkout");

  useEffect(() => {
    Promise.all([getMe(), getPlan()])
      .then(([u, p]) => { setUser(u); setPlan(p); })
      .catch(() => setError("Failed to load profile"));
  }, []);

  if (error) return <p className="p-10 text-red-600">{error}</p>;
  if (!user || !plan) return <p className="p-10 text-gray-400">Loading…</p>;

  const isPro            = plan.plan === "pro";
  const searchPct        = plan.searchLimit === -1 ? 0
                         : Math.min(100, Math.round((plan.searchCount / plan.searchLimit) * 100));
  const candidatePct     = plan.candidateLimit === -1 ? 0
                         : Math.min(100, Math.round((plan.candidateCount / plan.candidateLimit) * 100));

  async function handleUpgrade() {
    setLoading(true);
    try { await startCheckout(); } catch { setLoading(false); }
  }

  async function handlePortal() {
    setLoading(true);
    try { await openBillingPortal(); } catch { setLoading(false); }
  }

  return (
    <div className="max-w-lg mx-auto px-4 py-10">
      <h1 className="text-2xl font-bold mb-6">Profile</h1>

      {/* Checkout success/cancel banners */}
      {checkoutResult === "success" && (
        <div className="mb-4 px-4 py-3 bg-green-50 border border-green-200 rounded text-green-700 text-sm">
          Subscription activated — you are now on the Pro plan!
        </div>
      )}
      {checkoutResult === "cancel" && (
        <div className="mb-4 px-4 py-3 bg-gray-50 border border-gray-200 rounded text-gray-500 text-sm">
          Checkout cancelled. You are still on the Free plan.
        </div>
      )}

      {/* Account info */}
      <div className="bg-white border border-gray-200 rounded-lg divide-y divide-gray-100 mb-6">
        <Row label="Username" value={user.name} />
        <Row label="Email"    value={user.email} />
        <Row
          label="Roles"
          value={
            <span className="flex gap-2">
              {user.roles.map((r) => (
                <span key={r} className="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded">
                  {r.replace("ROLE_", "")}
                </span>
              ))}
            </span>
          }
        />
      </div>

      {/* Plan card */}
      <div className={`rounded-lg border p-5 mb-4 ${
        isPro ? "border-purple-300 bg-purple-50" : "border-gray-200 bg-white"
      }`}>
        <div className="flex items-center justify-between mb-4">
          <div>
            <span className={`text-xs font-semibold uppercase tracking-wide px-2 py-0.5 rounded ${
              isPro ? "bg-purple-600 text-white" : "bg-gray-200 text-gray-600"
            }`}>
              {isPro ? "Pro" : "Free"}
            </span>
            {isPro && (
              <p className="text-xs text-gray-500 mt-1">Unlimited searches &amp; candidates</p>
            )}
          </div>
          {!isPro && (
            <button
              onClick={handleUpgrade}
              disabled={loading}
              className="bg-purple-600 hover:bg-purple-700 text-white text-sm font-medium px-4 py-2 rounded disabled:opacity-60"
            >
              {loading ? "Redirecting…" : "Upgrade to Pro — $29/mo"}
            </button>
          )}
          {isPro && (
            <button
              onClick={handlePortal}
              disabled={loading}
              className="text-sm text-purple-700 border border-purple-300 px-3 py-1.5 rounded hover:bg-purple-100 disabled:opacity-60"
            >
              {loading ? "Redirecting…" : "Manage subscription"}
            </button>
          )}
        </div>

        {/* Usage bars — only meaningful on Free */}
        {!isPro && (
          <div className="space-y-3">
            <UsageBar
              label="Searches this month"
              used={plan.searchCount}
              limit={plan.searchLimit}
              pct={searchPct}
            />
            <UsageBar
              label="Saved candidates"
              used={plan.candidateCount}
              limit={plan.candidateLimit}
              pct={candidatePct}
            />
          </div>
        )}
      </div>

      {/* Free plan feature comparison */}
      {!isPro && (
        <div className="bg-white border border-gray-200 rounded-lg p-4 text-sm text-gray-600">
          <p className="font-medium text-gray-800 mb-2">Upgrade to Pro to unlock:</p>
          <ul className="space-y-1">
            <li>Unlimited searches per month</li>
            <li>Unlimited saved candidates</li>
            <li>Priority support</li>
          </ul>
        </div>
      )}
    </div>
  );
}

// ── Sub-components ─────────────────────────────────────────────────────────────

function Row({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex items-center px-4 py-3 gap-4">
      <span className="w-28 text-sm text-gray-500 shrink-0">{label}</span>
      <span className="text-sm font-medium">{value}</span>
    </div>
  );
}

function UsageBar({
  label, used, limit, pct,
}: {
  label: string; used: number | string; limit: number; pct: number;
}) {
  const barColor = pct >= 90 ? "bg-red-500" : pct >= 70 ? "bg-amber-400" : "bg-blue-500";
  return (
    <div>
      <div className="flex justify-between text-xs text-gray-500 mb-1">
        <span>{label}</span>
        <span>{used} / {limit}</span>
      </div>
      <div className="h-1.5 bg-gray-200 rounded-full overflow-hidden">
        <div className={`h-full rounded-full ${barColor}`} style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}
