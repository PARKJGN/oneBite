import React from 'react';
import { Alert, Text, TouchableOpacity, View } from 'react-native';
import { SocialLoginResult, useSocialLogin } from '../api/auth';
import { signInWithGoogle, signInWithKakao, signInWithNaver } from '../lib/social';

const PROVIDERS: { key: 'kakao' | 'naver' | 'google'; label: string; bg: string; fg: string; border?: boolean }[] = [
  { key: 'kakao', label: '카카오로 계속하기', bg: '#FEE500', fg: '#3c1e1e' },
  { key: 'naver', label: '네이버로 계속하기', bg: '#03C75A', fg: '#ffffff' },
  { key: 'google', label: 'Google로 계속하기', bg: '#ffffff', fg: '#1f1f1f', border: true },
];

export default function SocialButtons({ onSuccess }: { onSuccess: (r: SocialLoginResult) => void }) {
  const social = useSocialLogin();
  const fail = () => Alert.alert('로그인 실패', '소셜 로그인에 실패했어요. 다시 시도해 주세요.');

  const press = async (key: 'kakao' | 'naver' | 'google') => {
    try {
      const accessToken =
        key === 'google' ? await signInWithGoogle()
          : key === 'kakao' ? await signInWithKakao()
            : await signInWithNaver();
      social.mutate({ provider: key, accessToken }, { onSuccess, onError: fail });
    } catch {
      Alert.alert('로그인 취소', '소셜 로그인이 취소되었어요.');
    }
  };

  return (
    <View style={{ gap: 10 }}>
      {PROVIDERS.map((p) => (
        <TouchableOpacity key={p.key} onPress={() => press(p.key)} disabled={social.isPending}
          style={{
            paddingVertical: 13, borderRadius: 12, alignItems: 'center', backgroundColor: p.bg,
            borderWidth: p.border ? 1 : 0, borderColor: '#dcd7cf',
          }}>
          <Text style={{ fontSize: 14, fontWeight: '600', color: p.fg }}>{p.label}</Text>
        </TouchableOpacity>
      ))}
    </View>
  );
}
