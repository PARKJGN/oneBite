import * as WebBrowser from 'expo-web-browser';
import { Alert } from 'react-native';

/**
 * 법적 고지 문서는 웹(Next.js)에 단일 출처로 두고 앱은 인앱 브라우저로 띄운다.
 * Apple 은 앱 "내부"에서 개인정보처리방침에 닿을 수 있어야 심사를 통과시킨다.
 */
const WEB_ORIGIN = process.env.EXPO_PUBLIC_ONEBITE_WEB_URL ?? 'https://onebite.jgbak-land.com';

export const TERMS_URL = `${WEB_ORIGIN}/terms`;
export const PRIVACY_URL = `${WEB_ORIGIN}/privacy`;
export const ACCOUNT_DELETION_URL = `${WEB_ORIGIN}/account-deletion`;

export async function openLegal(url: string) {
  try {
    await WebBrowser.openBrowserAsync(url);
  } catch {
    Alert.alert('열 수 없음', `문서를 열 수 없어요.\n${url}`);
  }
}
