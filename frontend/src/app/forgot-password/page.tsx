"use client";

import { useState } from "react";
import Link from "next/link";
import { forgotPassword } from "@/lib/auth";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      await forgotPassword(email);
      setSent(true);
    } catch {
      setError("Something went wrong. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex items-center justify-center min-h-[calc(100vh-57px)]">
      <div className="w-full max-w-sm bg-white border border-gray-200 rounded-lg p-8 shadow-sm">
        <h1 className="text-xl font-bold mb-2">Reset password</h1>

        {sent ? (
          <div>
            <p className="text-sm text-gray-600 mb-4">
              If <span className="font-medium">{email}</span> is registered, a
              6-digit reset code has been sent. Check your inbox.
            </p>
            <Link
              href="/reset-password"
              className="block w-full text-center bg-blue-600 text-white py-2 rounded text-sm font-medium hover:bg-blue-700"
            >
              Enter reset code →
            </Link>
          </div>
        ) : (
          <>
            <p className="text-sm text-gray-500 mb-6">
              Enter the email address linked to your account and we'll send you a
              reset code.
            </p>
            <form onSubmit={handleSubmit} className="flex flex-col gap-4">
              <div>
                <label className="block text-sm font-medium mb-1">Email</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              {error && <p className="text-red-600 text-sm">{error}</p>}

              <button
                type="submit"
                disabled={loading}
                className="bg-blue-600 text-white py-2 rounded text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
              >
                {loading ? "Sending…" : "Send reset code"}
              </button>
            </form>
          </>
        )}

        <p className="text-sm text-gray-400 mt-4 text-center">
          <Link href="/login" className="hover:underline">
            Back to sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
