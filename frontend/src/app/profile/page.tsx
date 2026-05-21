"use client";

import { useEffect, useState } from "react";
import { getMe, UserInfo } from "@/lib/auth";

export default function ProfilePage() {
  const [user, setUser] = useState<UserInfo | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    getMe()
      .then(setUser)
      .catch(() => setError("Failed to load profile"));
  }, []);

  if (error) return <p className="p-10 text-red-600">{error}</p>;
  if (!user) return <p className="p-10 text-gray-400">Loading…</p>;

  return (
    <div className="max-w-lg mx-auto px-4 py-10">
      <h1 className="text-2xl font-bold mb-6">Profile</h1>

      <div className="bg-white border border-gray-200 rounded-lg divide-y divide-gray-100">
        <Row label="Username" value={user.name} />
        <Row label="Email" value={user.email} />
        <Row
          label="Roles"
          value={
            <span className="flex gap-2">
              {user.roles.map((r) => (
                <span
                  key={r}
                  className="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded"
                >
                  {r.replace("ROLE_", "")}
                </span>
              ))}
            </span>
          }
        />
      </div>
    </div>
  );
}

function Row({
  label,
  value,
}: {
  label: string;
  value: React.ReactNode;
}) {
  return (
    <div className="flex items-center px-4 py-3 gap-4">
      <span className="w-28 text-sm text-gray-500 shrink-0">{label}</span>
      <span className="text-sm font-medium">{value}</span>
    </div>
  );
}
