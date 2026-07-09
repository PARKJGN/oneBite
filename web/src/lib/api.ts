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

// 표준 봉투: { code, message, requestId, data } — 성공 시 실제 페이로드를 벗겨낸다.
function unwrap<T = unknown>(body: unknown): T {
  if (body && typeof body === 'object' && 'code' in body && 'data' in body) {
    return (body as { data: T }).data;
  }
  return body as T;
}

async function refreshAccess(): Promise<string | null> {
  const { refreshToken } = useSession.getState();
  if (!refreshToken) return null;
  try {
    // 인터셉터 재귀를 피하려고 bare axios로 호출(Authorization 미부착).
    // bare axios엔 언래핑 인터셉터가 없으므로 여기서 직접 봉투를 벗긴다.
    const res = await axios.post(`${baseURL}/auth/refresh`, { refreshToken });
    const data = unwrap<{ token: string; refreshToken: string }>(res.data);
    useSession.getState().setTokens(data.token, data.refreshToken);
    return data.token;
  } catch {
    return null;
  }
}

// 성공 응답 언래핑: 봉투면 data로 치환, 아니면(빈 응답 등) 그대로 통과.
// 기존 훅들이 res.data로 실제 페이로드를 읽던 코드가 그대로 동작한다.
api.interceptors.response.use(
  (res) => {
    res.data = unwrap(res.data);
    return res;
  },
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
      // 여기서 하드 이동하므로(AuthGuard의 replace보다 먼저 실행됨) 복귀 경로를 직접 붙인다.
      useSession.getState().clear();
      if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
        const next = encodeURIComponent(`${window.location.pathname}${window.location.search}`);
        window.location.href = `/login?next=${next}`;
      }
    }
    return Promise.reject(error);
  },
);
