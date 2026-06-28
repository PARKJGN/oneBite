import axios from 'axios';
import { useSession } from '@/store/session';

const baseURL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';
export const api = axios.create({ baseURL, timeout: 10000 });

// 요청: JWT access 토큰을 Authorization 헤더로 전달.
api.interceptors.request.use((config) => {
  const { token } = useSession.getState();
  if (token) config.headers.set('Authorization', `Bearer ${token}`);
  return config;
});

// access 만료(401) → refresh 토큰으로 1회 재발급(회전) 후 원요청 재시도.
// 동시 401이 몰려도 refresh는 한 번만(single-flight) 수행한다.
let refreshing: Promise<string | null> | null = null;

async function refreshAccess(): Promise<string | null> {
  const { refreshToken } = useSession.getState();
  if (!refreshToken) return null;
  try {
    // 인터셉터 재귀를 피하려고 bare axios로 호출(Authorization 미부착).
    const { data } = await axios.post(`${baseURL}/auth/refresh`, { refreshToken });
    useSession.getState().setTokens(data.token, data.refreshToken);
    return data.token as string;
  } catch {
    return null;
  }
}

api.interceptors.response.use(
  (r) => r,
  async (error) => {
    const original = error.config;
    const status = error.response?.status;
    // /auth/* 는 재발급 대상 아님(로그인 실패 등). 이미 1회 재시도한 요청도 제외.
    if (status === 401 && original && !original._retried && !String(original.url).includes('/auth/')) {
      original._retried = true;
      refreshing = refreshing ?? refreshAccess();
      const newToken = await refreshing;
      refreshing = null;
      if (newToken) {
        original.headers = original.headers ?? {};
        original.headers.Authorization = `Bearer ${newToken}`;
        return api(original);
      }
      // refresh 실패(만료/폐기) → 세션 정리 후 로그인으로 유도.
      useSession.getState().clear();
      if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  },
);
