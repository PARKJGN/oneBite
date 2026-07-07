import axios from 'axios';
import { Platform } from 'react-native';
import { useSession } from '../store/session';
import { resetToLogin } from '../lib/nav';

// 플랫폼별 기본 백엔드 주소.
//  - 웹(브라우저): localhost
//  - Android 에뮬레이터: 10.0.2.2 (에뮬레이터→호스트 별칭)
//  - 실기기(Expo Go): EXPO_PUBLIC_ONEBITE_API_URL 에 PC의 LAN IP를 지정(예: http://192.168.0.10:8080)
const defaultBase = Platform.OS === 'android' ? 'http://10.0.2.2:8080' : 'http://localhost:8080';
const baseURL = process.env.EXPO_PUBLIC_ONEBITE_API_URL ?? defaultBase;
export const api = axios.create({ baseURL, timeout: 10000 });

// 요청: 로그인 시 받은 JWT access 토큰을 Authorization 헤더로 전달.
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
    // bare axios라 봉투 언래핑 인터셉터를 타지 않으므로, 봉투(data.data)에서 직접 토큰을 꺼낸다.
    const { data } = await axios.post(`${baseURL}/auth/refresh`, { refreshToken });
    const payload = data?.data ?? data;
    useSession.getState().setTokens(payload.token, payload.refreshToken);
    return payload.token as string;
  } catch {
    return null;
  }
}

api.interceptors.response.use(
  (r) => {
    // 표준 봉투({ code, message, requestId, data }) 이면 페이로드로 언래핑 →
    // 기존 코드가 res.data 로 실제 페이로드를 읽던 동작을 유지한다.
    const body = r.data;
    if (body && typeof body === 'object' && 'code' in body && 'data' in body) {
      r.data = body.data;
    }
    return r;
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
      // refresh 실패(만료/폐기) → 세션 정리 후 로그인으로 복귀.
      useSession.getState().clear();
      resetToLogin();
    }
    return Promise.reject(error);
  },
);
