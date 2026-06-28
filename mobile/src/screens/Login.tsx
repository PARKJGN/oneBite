import React, { useState } from 'react';
import { Alert, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { useLogin } from '../api/auth';
import { useSession } from '../store/session';
import SocialButtons from '../components/SocialButtons';
import { colors, radius } from '../theme';

export default function Login({ navigation }: any) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const login = useLogin();
  const setSession = useSession((s) => s.setSession);

  const onLogin = () => {
    login.mutate(
      { username, password },
      {
        onSuccess: (r) => {
          setSession(r.userId, r.token, r.refreshToken);
          navigation.replace('Main');
        },
        onError: () => Alert.alert('로그인 실패', '아이디 또는 비밀번호를 확인해 주세요.'),
      },
    );
  };

  return (
    <View style={{ flex: 1, backgroundColor: colors.bg, padding: 32, justifyContent: 'center' }}>
      <Text style={{ fontSize: 42, fontWeight: '700', color: colors.ink }}>oneBite</Text>
      <Text style={{ fontSize: 17, color: colors.inkSoft, marginTop: 10 }}>하루 한 입, 깊이 있는 뉴스</Text>

      <View style={{ marginTop: 40, gap: 12 }}>
        <TextInput
          placeholder="아이디" autoCapitalize="none" value={username} onChangeText={setUsername}
          style={inputStyle}
        />
        <TextInput
          placeholder="비밀번호" secureTextEntry value={password} onChangeText={setPassword}
          style={inputStyle}
        />
      </View>

      <TouchableOpacity onPress={onLogin} disabled={login.isPending}
        style={{ marginTop: 16, backgroundColor: colors.green, borderRadius: radius.md, padding: 16, alignItems: 'center' }}>
        <Text style={{ color: '#fff', fontSize: 16, fontWeight: '600' }}>
          {login.isPending ? '로그인 중…' : '로그인'}
        </Text>
      </TouchableOpacity>

      <TouchableOpacity onPress={() => navigation.navigate('Signup')}
        style={{ marginTop: 16, backgroundColor: colors.surface, borderWidth: 1, borderColor: '#cdddd2', borderRadius: radius.md, padding: 16, alignItems: 'center' }}>
        <Text style={{ color: colors.green, fontSize: 16, fontWeight: '600' }}>회원가입</Text>
      </TouchableOpacity>

      <View style={{ flexDirection: 'row', alignItems: 'center', gap: 12, marginVertical: 22 }}>
        <View style={{ flex: 1, height: 1, backgroundColor: '#e2e9e3' }} />
        <Text style={{ fontSize: 12, color: colors.inkFaint }}>또는</Text>
        <View style={{ flex: 1, height: 1, backgroundColor: '#e2e9e3' }} />
      </View>

      <SocialButtons onSuccess={(r) => {
        setSession(r.userId, r.token, r.refreshToken);
        navigation.replace(r.isNew ? 'Onboard' : 'Main');
      }} />

      <Text style={{ fontSize: 12.5, color: colors.inkFaint, textAlign: 'center', marginTop: 18, lineHeight: 18 }}>
        소셜 로그인 또는 아이디·비밀번호·닉네임으로 시작하세요.
      </Text>
    </View>
  );
}

const inputStyle = {
  borderWidth: 1, borderColor: colors.border, borderRadius: radius.md,
  paddingVertical: 15, paddingHorizontal: 16, fontSize: 15, backgroundColor: colors.surface, color: colors.ink,
} as const;
