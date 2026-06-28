// 웹(Expo web) 스텁 — 네이티브 소셜 SDK가 없으므로 dev 플레이스홀더 토큰을 반환.
// 백엔드 PlaceholderSocialVerifier가 "providerId[:nickname]" 형식으로 해석한다.
export type SocialProvider = 'google' | 'naver' | 'kakao';

export async function signInWithGoogle(): Promise<string> {
  return 'dev-google-uid:테스터';
}
export async function signInWithKakao(): Promise<string> {
  return 'dev-kakao-uid:테스터';
}
export async function signInWithNaver(): Promise<string> {
  return 'dev-naver-uid:테스터';
}
