import { Linking } from 'react-native';
import * as Notifications from 'expo-notifications';
import { api } from '../api/client';

// 점진적 권한 요청(원칙 III, FR-013).

export type PermissionStatus = 'granted' | 'denied' | 'unknown';

// 플랫폼 권한 요청 어댑터. 기본은 expo-notifications, 테스트/대체 구현은 register로 주입 가능.
let nativeRequest: () => Promise<PermissionStatus> = async () => {
  const { status } = await Notifications.requestPermissionsAsync();
  return status === 'granted' ? 'granted' : 'denied';
};
export function registerNativePermissionRequest(fn: () => Promise<PermissionStatus>) {
  nativeRequest = fn;
}

/**
 * OS 권한 상태를 서버에 반영(FR-010).
 * 발송 대상 선정이 이 값을 쓰므로(EditionPersistenceAdapter 의 findByPushPermission("granted")),
 * 동기화가 빠지면 사용자가 권한을 허용해도 8시 푸시 대상에서 제외된다.
 * 실패는 조용히 무시한다 — 앱을 다시 켤 때 재동기화된다.
 */
export async function syncPushPermission(status: PermissionStatus): Promise<void> {
  try {
    await api.patch('/me', { pushPermission: status });
  } catch {
    // 무시
  }
}

// 권한을 요청하고 결과를 서버에 동기화(서버 동의 게이트 반영, FR-010).
export async function requestPushPermission(): Promise<PermissionStatus> {
  const status = await nativeRequest();
  await syncPushPermission(status);
  return status;
}

// 반복 거부 시 OS 설정으로 이동 안내(기능은 차단하지 않음).
export function openOsSettings() {
  Linking.openSettings().catch(() => {});
}
