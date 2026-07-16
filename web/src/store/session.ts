'use client';
import { useEffect, useState } from 'react';
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

// persist 가 localStorage 를 복원(hydrate) 했는지 추적하는 공용 훅.
// 복원 전엔 userId 가 항상 null 이라, 이 시점에 인증 판단을 하면 로그인 사용자가 오인된다.
// AuthGuard(본문)·AppChrome(네비) 이 동일 기준으로 렌더되도록 한 곳에서 제공한다.
export function useSessionHydrated() {
  const [hydrated, setHydrated] = useState(false);
  useEffect(() => {
    setHydrated(useSession.persist.hasHydrated());
    return useSession.persist.onFinishHydration(() => setHydrated(true));
  }, []);
  return hydrated;
}
