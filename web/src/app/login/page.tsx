'use client';
import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useLogin, useSocialLogin } from '@/lib/hooks';
import { useSession } from '@/store/session';
import { Button } from '@/components/ui/button';
import { GoogleButton } from '@/components/GoogleButton';

const googleConfigured = !!process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID;
const kakaoKey = process.env.NEXT_PUBLIC_KAKAO_REST_KEY;
const naverId = process.env.NEXT_PUBLIC_NAVER_CLIENT_ID;

export default function LoginPage() {
  const router = useRouter();
  const setSession = useSession((s) => s.setSession);
  const login = useLogin();
  const social = useSocialLogin();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  const onLogin = () =>
    login.mutate({ username, password }, {
      onSuccess: (r) => { setSession(r.userId, r.token, r.refreshToken); router.push('/'); },
      onError: () => alert('아이디 또는 비밀번호를 확인해 주세요.'),
    });

  // 제공자 access 토큰으로 백엔드 검증 → 세션 저장 → 이동
  const loginWithToken = (provider: string, accessToken: string) =>
    social.mutate({ provider, accessToken }, {
      onSuccess: (r) => { setSession(r.userId, r.token, r.refreshToken); router.push(r.isNew ? '/onboard' : '/'); },
      onError: () => alert('소셜 로그인에 실패했어요.'),
    });

  // 미구성 시 dev 토큰("providerId:nickname")
  const onDevSocial = (provider: string) => loginWithToken(provider, `dev-${provider}-uid:테스터`);

  // 카카오/네이버: authorize로 리다이렉트 → /oauth/[provider] 콜백에서 code 교환
  const startKakao = () => {
    const redirect = encodeURIComponent(`${window.location.origin}/oauth/kakao`);
    window.location.href = `https://kauth.kakao.com/oauth/authorize?client_id=${kakaoKey}&redirect_uri=${redirect}&response_type=code`;
  };
  const startNaver = () => {
    const redirect = encodeURIComponent(`${window.location.origin}/oauth/naver`);
    const state = Math.random().toString(36).slice(2); // CSRF state
    window.location.href = `https://nid.naver.com/oauth2.0/authorize?response_type=code&client_id=${naverId}&redirect_uri=${redirect}&state=${state}`;
  };

  return (
    <main className="mx-auto flex min-h-screen max-w-sm flex-col justify-center px-8">
      <h1 className="text-4xl font-bold text-ink">oneBite</h1>
      <p className="mt-2 text-ink-soft">하루 한 입, 깊이 있는 뉴스</p>

      <div className="mt-10 flex flex-col gap-3">
        <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="아이디"
          className="h-12 rounded-md border border-input bg-card px-4 text-ink outline-none focus:ring-2 focus:ring-ring" />
        <input value={password} onChange={(e) => setPassword(e.target.value)} type="password" placeholder="비밀번호"
          className="h-12 rounded-md border border-input bg-card px-4 text-ink outline-none focus:ring-2 focus:ring-ring" />
      </div>

      <Button className="mt-4 w-full" onClick={onLogin} disabled={login.isPending}>
        {login.isPending ? '로그인 중…' : '로그인'}
      </Button>

      <div className="mt-4 flex items-center justify-between text-sm">
        <Link href="/signup" className="font-semibold text-pine">회원가입</Link>
        <Link href="/reset/request" className="text-ink-faint hover:text-ink-soft">비밀번호를 잊으셨나요?</Link>
      </div>

      <div className="my-6 flex items-center gap-3">
        <div className="h-px flex-1 bg-border" /><span className="text-xs text-ink-faint">또는</span><div className="h-px flex-1 bg-border" />
      </div>

      <div className="flex flex-col gap-2.5">
        <button onClick={() => (kakaoKey ? startKakao() : onDevSocial('kakao'))} disabled={social.isPending}
          className="h-12 rounded-md bg-[#FEE500] text-sm font-semibold text-[#3c1e1e]">카카오로 계속하기</button>
        <button onClick={() => (naverId ? startNaver() : onDevSocial('naver'))} disabled={social.isPending}
          className="h-12 rounded-md bg-[#03C75A] text-sm font-semibold text-white">네이버로 계속하기</button>
        {googleConfigured ? (
          <GoogleButton onToken={(t) => loginWithToken('google', t)} disabled={social.isPending} />
        ) : (
          <button onClick={() => onDevSocial('google')} disabled={social.isPending}
            className="h-12 rounded-md border border-input bg-white text-sm font-semibold text-ink">Google로 계속하기</button>
        )}
      </div>

      <p className="mt-5 text-center text-xs leading-relaxed text-ink-faint">
        소셜 로그인 또는 아이디·비밀번호·닉네임으로 시작하세요.
      </p>
    </main>
  );
}
