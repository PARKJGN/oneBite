'use client';
import { usePathname } from 'next/navigation';
import { AppNav } from './AppNav';

// 루트 레이아웃에 "한 번만" 마운트되는 앱 크롬(헤더 + 하단 탭바).
// 페이지가 아니라 여기서 렌더하므로 라우트 전환에도 언마운트되지 않아 네비가 튀지 않는다.
// 인증 화면과, 스토어 심사자가 비로그인으로 방문하는 법적 고지 문서에서는 숨긴다.
const NO_NAV = [
  '/login', '/signup', '/onboard', '/oauth', '/reset',
  '/privacy', '/terms', '/account-deletion',
];

export function AppChrome() {
  const path = usePathname();
  const hidden = NO_NAV.some((p) => path === p || path.startsWith(p + '/'));
  if (hidden) return null;
  return <AppNav />;
}
