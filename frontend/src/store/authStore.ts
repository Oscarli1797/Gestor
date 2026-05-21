"use client";

import { create } from "zustand";
import { UserInfo, getMe, logout as doLogout } from "@/lib/auth";
import { getToken } from "@/lib/api";

interface AuthState {
  user: UserInfo | null;
  loading: boolean;
  hydrate: () => Promise<void>;
  logout: () => Promise<void>;
  setUser: (user: UserInfo | null) => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  loading: true,

  hydrate: async () => {
    if (!getToken()) {
      set({ loading: false });
      return;
    }
    try {
      const user = await getMe();
      set({ user, loading: false });
    } catch {
      set({ user: null, loading: false });
    }
  },

  logout: async () => {
    await doLogout();
    set({ user: null });
  },

  setUser: (user) => set({ user }),
}));
