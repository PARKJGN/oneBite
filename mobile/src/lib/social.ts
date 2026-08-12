import { GoogleSignin, isSuccessResponse } from '@react-native-google-signin/google-signin';
import NaverLogin from '@react-native-seoul/naver-login';
import { login as kakaoLoginNative, logout as kakaoLogoutNative } from '@react-native-seoul/kakao-login';

export type SocialProvider = 'google' | 'naver' | 'kakao';

/**
 * 소셜 로그인 — 모바일은 전부 네이티브 SDK로 access 토큰을 얻어 반환한다.
 * 백엔드(HttpSocialIdentityVerifier)가 각 토큰을 제공자 UserInfo로 검증(FR-001c).
 * (웹은 social.web.ts 의 dev 폴백 스텁이 대신 쓰인다 — Metro가 .web.ts 우선 해석)
 */

// ── Google (@react-native-google-signin/google-signin) ──
let googleConfigured = false;
export async function signInWithGoogle(): Promise<string> {
  if (!googleConfigured) {
    GoogleSignin.configure({
      webClientId: process.env.EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID, // 안드로이드/토큰 audience
      iosClientId: process.env.EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID,
      offlineAccess: false,
    });
    googleConfigured = true;
  }
  await GoogleSignin.hasPlayServices();
  const response = await GoogleSignin.signIn();
  if (!isSuccessResponse(response)) throw new Error('google_login_cancelled');
  const { accessToken } = await GoogleSignin.getTokens();
  if (!accessToken) throw new Error('google_token_failed');
  return accessToken;
}

// ── Naver (@react-native-seoul/naver-login) ──
let naverInited = false;
export async function signInWithNaver(): Promise<string> {
  if (!process.env.EXPO_PUBLIC_NAVER_CLIENT_ID) return 'dev-naver-uid:테스터'; // 미구성 dev 폴백
  if (!naverInited) {
    NaverLogin.initialize({
      appName: 'oneBite',
      consumerKey: process.env.EXPO_PUBLIC_NAVER_CLIENT_ID ?? '',
      consumerSecret: process.env.EXPO_PUBLIC_NAVER_CLIENT_SECRET ?? '',
      serviceUrlSchemeIOS: 'onebite', // app.json 플러그인 urlScheme과 일치
      disableNaverAppAuthIOS: true,
    });
    naverInited = true;
  }
  const { successResponse, failureResponse } = await NaverLogin.login();
  if (!successResponse?.accessToken) throw new Error(failureResponse?.message ?? 'naver_login_failed');
  return successResponse.accessToken;
}

// ── Kakao (@react-native-seoul/kakao-login) — 네이티브 앱 키는 app.json 플러그인에 설정 ──
export async function signInWithKakao(): Promise<string> {
  const token = await kakaoLoginNative();
  if (!token?.accessToken) throw new Error('kakao_login_failed');
  return token.accessToken;
}

/**
 * 앱 로그아웃 시 소셜 SDK 토큰도 함께 정리한다.
 * 이걸 안 하면 로그아웃해도 SDK에 토큰이 남아 같은 계정으로 즉시 재로그인되고,
 * 사용자가 앱 안에서 계정을 바꿀 방법이 없어진다(공용 기기면 앞사람 계정으로 들어간다).
 *
 * 제공자별로 독립 실행한다 — 한 곳이 실패해도 나머지는 정리되어야 하고,
 * 애초에 그 제공자로 로그인한 적이 없으면(초기화 전) 던지는 것이 정상이다.
 * 브라우저 쿠키(iOS는 Safari 세션 공유)까지는 지울 수 없어 계정 선택 화면이
 * 안 뜰 수 있다. 그 경우 브라우저에서 직접 로그아웃해야 한다.
 */
export async function signOutSocial(): Promise<void> {
  await Promise.allSettled([
    GoogleSignin.signOut(),
    NaverLogin.logout(),
    kakaoLogoutNative(),
  ]);
}
