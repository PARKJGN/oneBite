import { api } from '../api/client';

// 기기 타임존을 캡처해 서버 User.timezone에 저장(T036a, FR-019/원칙 XI).
// Hermes의 Intl 지원으로 IANA 타임존 문자열을 얻는다.
export function deviceTimezone(): string {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Seoul';
  } catch {
    return 'Asia/Seoul'; // 안전한 기본값(원칙 XI 폴백)
  }
}

export async function syncTimezone(): Promise<void> {
  await api.patch('/me', { timezone: deviceTimezone() });
}
