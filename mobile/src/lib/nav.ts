import type { NavigationContainerRef } from '@react-navigation/native';

/**
 * 전역 내비게이션 참조 + 라우팅 헬퍼.
 * (client/push 양쪽에서 쓰여 순환 import를 피하려고 별도 모듈로 분리)
 */
export type PushData = { type?: 'edition' | 'today'; editionId?: string | number };

let navRef: NavigationContainerRef<any> | null = null;

export function registerNav(ref: NavigationContainerRef<any> | null) {
  navRef = ref;
}

// 세션 만료(refresh 실패) 시 스택을 비우고 로그인 화면으로 복귀.
export function resetToLogin() {
  if (navRef?.isReady()) {
    navRef.resetRoot({ index: 0, routes: [{ name: 'Login' }] });
  }
}

// 푸시 페이로드 → 에디션 딥링크 라우팅(FR-009). 8시 묶음 푸시에 editionId(또는 today)가 담긴다.
export function handlePushData(data: PushData | undefined) {
  if (!navRef?.isReady() || !data) return;
  if (data.type === 'edition' && data.editionId != null) {
    navRef.navigate('EditionDetail', { id: Number(data.editionId) });
  } else {
    navRef.navigate('Main'); // 기본: 오늘 탭
  }
}
