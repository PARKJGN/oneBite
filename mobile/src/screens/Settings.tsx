import React from 'react';
import { Alert, ScrollView, Text, TouchableOpacity, View } from 'react-native';
import { useMe, useDeleteAccount } from '../api/account';
import { useSession } from '../store/session';
import { openOsSettings } from '../lib/permission';
import { openLegal, PRIVACY_URL, TERMS_URL } from '../lib/legal';
import { signOutSocial } from '../lib/social';
import { colors, radius } from '../theme';

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <View style={{ marginTop: 24 }}>
      <Text style={{ fontSize: 12, fontWeight: '700', letterSpacing: 1, color: colors.inkFaint }}>{title}</Text>
      <View style={{ marginTop: 10, backgroundColor: colors.surface, borderRadius: radius.md, borderWidth: 1, borderColor: '#e6ede8' }}>
        {children}
      </View>
    </View>
  );
}
const Row = ({ children }: { children: React.ReactNode }) => (
  <View style={{ padding: 14, borderBottomWidth: 1, borderBottomColor: '#eef2ee' }}>{children}</View>
);

// 서버가 주는 원시값(granted|denied|unknown)을 그대로 보여주지 않는다.
// unknown 은 "아직 서버에 동기화되지 않음"이라 사용자에겐 미설정으로 읽히는 게 맞다.
const PUSH_LABEL: Record<string, string> = {
  granted: '허용',
  denied: '거부',
  unknown: '미설정',
};

export default function Settings({ navigation }: any) {
  const me = useMe();
  const clear = useSession((s) => s.clear);
  const deleteAccount = useDeleteAccount();

  const toLogin = () => navigation.reset({ index: 0, routes: [{ name: 'Login' }] });

  // 소셜 SDK 토큰까지 지워야 다음 로그인에서 계정을 다시 고를 수 있다.
  // SDK 정리를 기다리다 화면 전환이 늦어지지 않도록 로컬 세션부터 끊는다.
  const signOutAll = () => { clear(); toLogin(); void signOutSocial(); };

  const onLogout = signOutAll;

  const onDelete = () =>
    Alert.alert('회원 탈퇴', '개인정보·슬롯·읽음/책갈피가 삭제됩니다. (받은 뉴스 요약 본문은 익명 보존)\n탈퇴할까요?', [
      { text: '취소', style: 'cancel' },
      { text: '탈퇴', style: 'destructive', onPress: () => deleteAccount.mutate(undefined, { onSuccess: signOutAll }) },
    ]);

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.bg }} contentContainerStyle={{ padding: 22, paddingTop: 18, paddingBottom: 56 }}>
      <Text style={{ fontSize: 28, fontWeight: '700', color: colors.ink }}>설정</Text>

      <Section title="계정">
        <Row><Text style={{ color: colors.ink }}>닉네임  ·  {me.data?.nickname ?? '—'}</Text></Row>
        <Row><Text style={{ color: colors.ink }}>아이디  ·  {me.data?.username ?? '소셜 계정'}</Text></Row>
        <View style={{ padding: 14 }}>
          <Text style={{ color: colors.ink }}>복구 이메일  ·  {me.data?.recoveryEmail ?? '미설정'}</Text>
        </View>
      </Section>

      <Section title="알림">
        <Row><Text style={{ color: colors.ink }}>푸시 알림  ·  {PUSH_LABEL[me.data?.pushPermission ?? 'unknown'] ?? '확인 중'}</Text></Row>
        <TouchableOpacity onPress={openOsSettings} style={{ padding: 14 }}>
          <Text style={{ color: colors.green, fontWeight: '600' }}>기기 알림 설정 열기</Text>
        </TouchableOpacity>
      </Section>

      <Section title="정보">
        <Row><Text style={{ color: colors.inkSoft }}>버전 1.0.0</Text></Row>
        <TouchableOpacity onPress={() => openLegal(TERMS_URL)} style={{ padding: 14, borderBottomWidth: 1, borderBottomColor: '#eef2ee' }}>
          <Text style={{ color: colors.ink, fontWeight: '600' }}>이용약관</Text>
        </TouchableOpacity>
        <TouchableOpacity onPress={() => openLegal(PRIVACY_URL)} style={{ padding: 14 }}>
          <Text style={{ color: colors.ink, fontWeight: '600' }}>개인정보처리방침</Text>
        </TouchableOpacity>
      </Section>

      <Section title="위험 구역">
        <TouchableOpacity onPress={onLogout} style={{ padding: 14, borderBottomWidth: 1, borderBottomColor: '#eef2ee' }}>
          <Text style={{ color: colors.ink, fontWeight: '600' }}>로그아웃</Text>
        </TouchableOpacity>
        <TouchableOpacity onPress={onDelete} style={{ padding: 14 }}>
          <Text style={{ color: colors.danger, fontWeight: '600' }}>회원 탈퇴</Text>
        </TouchableOpacity>
      </Section>
    </ScrollView>
  );
}
