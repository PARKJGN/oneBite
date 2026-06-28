import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';
import { Platform } from 'react-native';
import * as SecureStore from 'expo-secure-store';

// 클라이언트 세션 상태(Zustand): 로그인 사용자·토큰·출력 언어.
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

// 영속 저장소: 네이티브는 SecureStore(암호화), 웹(Expo web)은 localStorage.
const storage = createJSONStorage(() =>
  Platform.OS === 'web'
    ? (globalThis as any).localStorage
    : {
        getItem: (k: string) => SecureStore.getItemAsync(k),
        setItem: (k: string, v: string) => SecureStore.setItemAsync(k, v),
        removeItem: (k: string) => SecureStore.deleteItemAsync(k),
      },
);

export const useSession = create<SessionState>()(
  persist(
    (set) => ({
      userId: null,
      token: null,
      refreshToken: null,
      language: 'ko', // 기본 한국어(FR-002a)
      setSession: (userId, token, refreshToken) => set({ userId, token, refreshToken }),
      setTokens: (token, refreshToken) => set({ token, refreshToken }),
      setLanguage: (language) => set({ language }),
      clear: () => set({ userId: null, token: null, refreshToken: null }),
    }),
    { name: 'onebite_session', storage },
  ),
);
