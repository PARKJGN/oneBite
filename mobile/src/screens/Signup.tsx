import React, { useState } from 'react';
import { Alert, ScrollView, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { useCheckNickname, useCheckUsername, useSignup } from '../api/auth';
import { useSession } from '../store/session';
import { colors, radius } from '../theme';

const RED = '#d14343';
const USERNAME_MIN = 4;
const USERNAME_MAX = 20;

export default function Signup({ navigation }: any) {
  const [nickname, setNickname] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [recoveryEmail, setRecoveryEmail] = useState('');
  const [usernameStatus, setUsernameStatus] = useState<'idle' | 'available' | 'taken' | 'invalid'>('idle');
  const [nicknameStatus, setNicknameStatus] = useState<'idle' | 'available' | 'taken' | 'invalid'>('idle');

  const signup = useSignup();
  const checkUsername = useCheckUsername();
  const checkNickname = useCheckNickname();
  const setSession = useSession((s) => s.setSession);

  const usernameValid = username.length >= USERNAME_MIN && username.length <= USERNAME_MAX;
  const nicknameValid = nickname.trim().length > 0;
  const passwordValid = password.length >= 8;
  const confirmMatch = confirm.length > 0 && confirm === password;

  const onCheckUsername = () => {
    if (!usernameValid) {
      setUsernameStatus('invalid');
      return;
    }
    checkUsername.mutate(username, {
      onSuccess: (available) => setUsernameStatus(available ? 'available' : 'taken'),
      onError: () => Alert.alert('확인 실패', '아이디 확인 중 오류가 발생했어요.'),
    });
  };

  const onCheckNickname = () => {
    if (!nicknameValid) {
      setNicknameStatus('invalid');
      return;
    }
    checkNickname.mutate(nickname.trim(), {
      onSuccess: (available) => setNicknameStatus(available ? 'available' : 'taken'),
      onError: () => Alert.alert('확인 실패', '닉네임 확인 중 오류가 발생했어요.'),
    });
  };

  const onSubmit = () => {
    if (!nicknameValid) { Alert.alert('확인', '닉네임을 입력해 주세요.'); return; }
    if (nicknameStatus !== 'available') { Alert.alert('확인', '닉네임 중복확인을 해주세요.'); return; }
    if (!usernameValid) { Alert.alert('확인', `아이디는 ${USERNAME_MIN}~${USERNAME_MAX}자여야 해요.`); return; }
    if (usernameStatus !== 'available') { Alert.alert('확인', '아이디 중복확인을 해주세요.'); return; }
    if (!passwordValid) { Alert.alert('확인', '비밀번호는 8자 이상이어야 해요.'); return; }
    if (!confirmMatch) { Alert.alert('확인', '비밀번호가 일치하지 않아요.'); return; }
    signup.mutate(
      { username, password, nickname, recoveryEmail: recoveryEmail.trim() || null },
      {
        onSuccess: (r) => {
          setSession(r.userId, r.token, r.refreshToken);
          navigation.replace('Onboard');
        },
        onError: (e: any) => {
          // 에러 응답은 봉투({ code, message, ... }) 그대로 담겨 옴(에러 경로는 언래핑 안 함).
          const code = e?.response?.data?.code;
          const message = e?.response?.data?.message;
          if (code === 'NICKNAME_ALREADY_EXISTS') setNicknameStatus('taken');
          else if (code === 'USERNAME_ALREADY_EXISTS' || e?.response?.status === 409) setUsernameStatus('taken');
          Alert.alert('가입 실패', message ?? e?.message ?? '입력값을 확인해 주세요.');
        },
      },
    );
  };

  // 입력 검증 상태에 따른 테두리 색
  const borderFor = (ok: boolean, bad: boolean) => (ok ? colors.green : bad ? RED : colors.border);

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.bg }} contentContainerStyle={{ padding: 32, paddingTop: 54 }}>
      <Text style={{ fontSize: 32, fontWeight: '700', color: colors.ink }}>회원가입</Text>
      <Text style={{ fontSize: 15, color: colors.inkSoft, marginTop: 9 }}>이메일·휴대폰 인증 없이 바로 시작하세요.</Text>

      <View style={{ marginTop: 28, gap: 16 }}>
        {/* 닉네임 + 중복확인 */}
        <View>
          <View style={{ flexDirection: 'row', gap: 8 }}>
            <TextInput
              placeholder="닉네임"
              value={nickname}
              onChangeText={(t) => { setNickname(t); setNicknameStatus('idle'); }}
              style={[inputBase, { flex: 1, borderColor: borderFor(nicknameStatus === 'available', nicknameStatus === 'taken' || nicknameStatus === 'invalid') }]}
            />
            <TouchableOpacity
              onPress={onCheckNickname}
              disabled={checkNickname.isPending || !nickname}
              style={{ paddingHorizontal: 16, justifyContent: 'center', borderRadius: radius.md, backgroundColor: colors.greenSoft }}>
              <Text style={{ color: colors.green, fontWeight: '600' }}>{checkNickname.isPending ? '확인 중…' : '중복확인'}</Text>
            </TouchableOpacity>
          </View>
          <Text style={{ marginTop: 6, fontSize: 12.5, color: statusColor(nicknameStatus) }}>
            {nicknameMsg(nicknameStatus)}
          </Text>
        </View>

        {/* 아이디 + 중복확인 */}
        <View>
          <View style={{ flexDirection: 'row', gap: 8 }}>
            <TextInput
              placeholder={`아이디 (${USERNAME_MIN}~${USERNAME_MAX}자)`} autoCapitalize="none"
              value={username}
              onChangeText={(t) => { setUsername(t); setUsernameStatus('idle'); }}
              style={[inputBase, { flex: 1, borderColor: borderFor(usernameStatus === 'available', usernameStatus === 'taken' || usernameStatus === 'invalid') }]}
            />
            <TouchableOpacity
              onPress={onCheckUsername}
              disabled={checkUsername.isPending || !username}
              style={{ paddingHorizontal: 16, justifyContent: 'center', borderRadius: radius.md, backgroundColor: colors.greenSoft }}>
              <Text style={{ color: colors.green, fontWeight: '600' }}>{checkUsername.isPending ? '확인 중…' : '중복확인'}</Text>
            </TouchableOpacity>
          </View>
          <Text style={{ marginTop: 6, fontSize: 12.5, color: statusColor(usernameStatus) }}>
            {usernameMsg(usernameStatus)}
          </Text>
        </View>

        {/* 비밀번호 */}
        <View>
          <TextInput
            placeholder="비밀번호 (8자 이상)" secureTextEntry value={password} onChangeText={setPassword}
            style={[inputBase, { borderColor: borderFor(passwordValid, password.length > 0 && !passwordValid) }]}
          />
          <Text style={{ marginTop: 6, fontSize: 12.5, color: passwordValid ? colors.green : colors.inkFaint }}>
            {passwordValid ? '✓ 사용 가능한 비밀번호' : '8자 이상 입력해 주세요'}
          </Text>
        </View>

        {/* 비밀번호 확인 */}
        <View>
          <TextInput
            placeholder="비밀번호 확인" secureTextEntry value={confirm} onChangeText={setConfirm}
            style={[inputBase, { borderColor: borderFor(confirmMatch, confirm.length > 0 && !confirmMatch) }]}
          />
          {confirm.length > 0 && (
            <Text style={{ marginTop: 6, fontSize: 12.5, color: confirmMatch ? colors.green : RED }}>
              {confirmMatch ? '✓ 비밀번호가 일치해요' : '비밀번호가 일치하지 않아요'}
            </Text>
          )}
        </View>

        {/* 복구 이메일 */}
        <TextInput placeholder="복구 이메일 (선택)" autoCapitalize="none" keyboardType="email-address"
          value={recoveryEmail} onChangeText={setRecoveryEmail} style={inputBase} />
      </View>

      {/* 복구 불가 고지(FR-001b) */}
      <View style={{ marginTop: 14, padding: 14, backgroundColor: colors.greenSoft, borderRadius: radius.md }}>
        <Text style={{ fontSize: 12.5, color: colors.inkSoft, lineHeight: 18 }}>
          복구 이메일을 입력하지 않으면 비밀번호를 잊었을 때 계정을 복구할 수 없어요.
        </Text>
      </View>

      <TouchableOpacity onPress={onSubmit} disabled={signup.isPending}
        style={{ marginTop: 18, backgroundColor: colors.green, borderRadius: radius.md, padding: 16, alignItems: 'center' }}>
        <Text style={{ color: '#fff', fontSize: 16, fontWeight: '600' }}>
          {signup.isPending ? '가입 중…' : '가입하고 슬롯 만들기'}
        </Text>
      </TouchableOpacity>

      <Text style={{ fontSize: 13, color: colors.inkSoft, textAlign: 'center', marginTop: 20 }}>
        이미 계정이 있으신가요?{' '}
        <Text onPress={() => navigation.navigate('Login')} style={{ color: colors.green, fontWeight: '600' }}>로그인</Text>
      </Text>
    </ScrollView>
  );
}

function usernameMsg(s: 'idle' | 'available' | 'taken' | 'invalid'): string {
  switch (s) {
    case 'available': return '✓ 사용 가능한 아이디예요';
    case 'taken': return '이미 사용 중인 아이디예요';
    case 'invalid': return `아이디는 ${USERNAME_MIN}~${USERNAME_MAX}자여야 해요`;
    default: return `${USERNAME_MIN}~${USERNAME_MAX}자, 가입 전 중복확인을 눌러주세요`;
  }
}
function nicknameMsg(s: 'idle' | 'available' | 'taken' | 'invalid'): string {
  switch (s) {
    case 'available': return '✓ 사용 가능한 닉네임이에요';
    case 'taken': return '이미 사용 중인 닉네임이에요';
    case 'invalid': return '닉네임을 입력해 주세요';
    default: return '가입 전 중복확인을 눌러주세요';
  }
}
function statusColor(s: 'idle' | 'available' | 'taken' | 'invalid'): string {
  if (s === 'available') return colors.green;
  if (s === 'taken' || s === 'invalid') return RED;
  return colors.inkFaint;
}

const inputBase = {
  borderWidth: 1, borderColor: colors.border, borderRadius: radius.md,
  paddingVertical: 15, paddingHorizontal: 16, fontSize: 15, backgroundColor: colors.surface, color: colors.ink,
} as const;
