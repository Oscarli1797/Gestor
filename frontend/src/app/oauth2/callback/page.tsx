"use client";

import { useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { saveToken } from "@/lib/api";
import { useAuthStore } from "@/store/authStore";

/**
 * The browser lands here after Google OAuth.
 * The backend redirects to: /oauth2/callback?token=<JWT>
 * or /oauth2/callback?error=<message> on failure.
 */
export default function OAuth2CallbackPage() {
  const router = useRouter();
  const params = useSearchParams();
  const { hydrate } = useAuthStore();

  useEffect(() => {
    const token = params.get("token");
    const error = params.get("error");

    if (token) {
      saveToken(token);
      hydrate().then(() => router.replace("/search"));
    } else {
      // OAuth failed — redirect to login with error in state
      router.replace(`/login?oauthError=${encodeURIComponent(error ?? "OAuth login failed")}`);
    }
  }, [params, router, hydrate]);

  return (
    <div className="flex items-center justify-center min-h-[calc(100vh-57px)]">
      <p className="text-sm text-gray-400">Signing you in…</p>
    </div>
  );
}
