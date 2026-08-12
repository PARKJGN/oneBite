import * as Notifications from 'expo-notifications';
import { Platform } from 'react-native';
import { api } from '../api/client';
import { syncPushPermission } from './permission';
import { handlePushData, PushData } from './nav';

// nav.ts로 이동한 항목들의 재노출(기존 import 경로 호환)
export { registerNav, resetToLogin, handlePushData } from './nav';
export type { PushData } from './nav';

// 포그라운드 알림 표시 방식(SDK 56: banner/list 플래그).
Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowBanner: true,
    shouldShowList: true,
    shouldPlaySound: false,
    shouldSetBadge: false,
  }),
});

/**
 * 푸시 권한 요청 → FCM/APNs 원시 디바이스 토큰 획득 → 서버 등록(POST /devices).
 * 백엔드 FcmPushSender가 이 원시 토큰으로 발송한다.
 *
 * 실 동작 조건: Android는 google-services.json + dev build, iOS는 APNs 인증서 + dev build.
 * Expo Go/미구성에서는 getDevicePushTokenAsync가 실패하므로 조용히 무시한다(앱은 계속 동작).
 */
export async function registerForPushNotifications(): Promise<void> {
  try {
    let { status } = await Notifications.getPermissionsAsync();
    if (status !== 'granted') {
      status = (await Notifications.requestPermissionsAsync()).status;
    }
    // 실제 OS 상태를 매 실행마다 서버와 맞춘다. 이게 없으면 서버가 unknown 으로 남아
    // 발송 대상에서 빠지고, OS 설정에서 나중에 켜고 끈 것도 반영되지 않는다.
    await syncPushPermission(status === 'granted' ? 'granted' : 'denied');

    if (status !== 'granted') return; // 거부 시 기능 차단 없이 종료(원칙 III)

    const deviceToken = await Notifications.getDevicePushTokenAsync(); // 원시 FCM/APNs 토큰
    const platform = Platform.OS === 'ios' ? 'ios' : 'android';
    await api.post('/devices', { token: String(deviceToken.data), platform });
  } catch {
    // Expo Go·자격증명 미구성 환경: dev build에서 동작. 여기선 무시.
  }
}

/**
 * 알림 탭 → 딥링크 라우팅 리스너 등록. 반환된 구독은 unmount 시 해제.
 * 콜드스타트(종료 상태에서 알림 탭) 진입도 처리.
 */
export function attachNotificationRouting(): () => void {
  const sub = Notifications.addNotificationResponseReceivedListener((response) => {
    handlePushData(response.notification.request.content.data as PushData);
  });
  // 종료 상태에서 알림으로 실행된 경우
  Notifications.getLastNotificationResponseAsync().then((response) => {
    if (response) handlePushData(response.notification.request.content.data as PushData);
  });
  return () => sub.remove();
}
