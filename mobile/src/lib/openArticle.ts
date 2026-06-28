import { Alert, Linking } from 'react-native';

/**
 * 원문 기사 열기 — 빈 링크/실패 시 조용히 무시하지 않고 사용자에게 피드백을 준다.
 * scheme(http/https)이 없으면 https://를 붙여 보정한다.
 */
export async function openArticle(url?: string | null) {
  if (!url || !url.trim()) {
    Alert.alert('링크 없음', '이 항목엔 원문 링크가 없어요.');
    return;
  }
  const target = /^https?:\/\//i.test(url) ? url : `https://${url}`;
  try {
    await Linking.openURL(target);
  } catch {
    Alert.alert('열 수 없음', `링크를 열 수 없어요.\n${target}`);
  }
}
