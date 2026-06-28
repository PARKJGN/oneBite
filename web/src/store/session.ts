'use client';
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

type Language = 'ko' | 'en';

interface SessionState {
  userId: number | null;
  token: string | null; // JWT access
  refreshToken: string | null; // opaque refresh(회전 대상)
  language: Language;
  setSession: (userId: number, token: string, refreshToken: string) => void;
  setTokens: (token: string, refreshToken: string) => void; // refresh 회전 시 갱신
  setLanguage: (language: Language) => void;
  clear: () => void;
}

export const useSession = create<SessionState>()(
  persist(
    (set) => ({
      userId: null,
      token: null,
      refreshToken: null,
      language: 'ko',
      setSession: (userId, token, refreshToken) => set({ userId, token, refreshToken }),
      setTokens: (token, refreshToken) => set({ token, refreshToken }),
      setLanguage: (language) => set({ language }),
      clear: () => set({ userId: null, token: null, refreshToken: null }),
    }),
    { name: 'onebite-session' },
  ),
);
