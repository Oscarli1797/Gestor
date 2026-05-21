"use client";

import Link from "next/link";
import { useEffect } from "react";
import { useAuthStore } from "@/store/authStore";

export default function Navbar() {
  const { user, loading, hydrate, logout } = useAuthStore();

  useEffect(() => {
    hydrate();
  }, [hydrate]);

  return (
    <header className="bg-white border-b border-gray-200 px-6 py-3 flex items-center justify-between">
      <Link href="/" className="text-lg font-semibold tracking-tight">
        GestorProyectos
      </Link>

      <nav className="flex items-center gap-4 text-sm">
        <Link href="/" className="hover:text-blue-600">
          Search
        </Link>

        {!loading && (
          <>
            {user ? (
              <>
                <Link href="/profile" className="hover:text-blue-600">
                  {user.name}
                </Link>
                {user.roles.includes("ROLE_ADMIN") && (
                  <span className="text-xs bg-red-100 text-red-700 px-2 py-0.5 rounded">
                    Admin
                  </span>
                )}
                <button
                  onClick={logout}
                  className="text-gray-500 hover:text-red-600"
                >
                  Logout
                </button>
              </>
            ) : (
              <>
                <Link href="/login" className="hover:text-blue-600">
                  Login
                </Link>
                <Link
                  href="/register"
                  className="bg-blue-600 text-white px-3 py-1.5 rounded hover:bg-blue-700"
                >
                  Register
                </Link>
              </>
            )}
          </>
        )}
      </nav>
    </header>
  );
}
