'use client';
import { usePathname } from 'next/navigation';
import { AppNav } from './AppNav';
import { useSession, useSessionHydrated } from '@/store/session';

// 루트 레이아웃에 "한 번만" 마운트되는 앱 크롬(헤더 + 하단 탭바).
// 페이지가 아니라 여기서 렌더하므로 라우트 전환에도 언마운트되지 않아 네비가 튀지 않는다.
// 인증 화면과, 스토어 심사자가 비로그인으로 방문하는 법적 고지 문서에서는 숨긴다.
const NO_NAV = [
  '/login', '/signup', '/onboard', '/oauth', '/reset',
  '/privacy', '/terms', '/account-deletion',
];

export function AppChrome() {
  const path = usePathname();
  const hydrated = useSessionHydrated();
  const userId = useSession((s) => s.userId);
  const hiddenRoute = NO_NAV.some((p) => path === p || path.startsWith(p + '/'));
  // 인증 화면·법적 고지에서 숨기고, 미인증(복원 전 포함) 상태에서도 네비를 감춘다.
  // AuthGuard 의 본문 렌더 기준(hydrated && userId)과 동일하게 맞춰 함께 나타난다.
  if (hiddenRoute || !hydrated || userId == null) return null;
  return <AppNav />;
}
