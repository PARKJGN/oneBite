'use client';
import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useSignup } from '@/lib/hooks';
import { useSession } from '@/store/session';
import { Button } from '@/components/ui/button';

const input = 'h-12 rounded-md border border-input bg-card px-4 text-ink outline-none focus:ring-2 focus:ring-ring';

export default function SignupPage() {
  const router = useRouter();
  const setSession = useSession((s) => s.setSession);
  const signup = useSignup();
  const [nickname, setNickname] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [recoveryEmail, setRecoveryEmail] = useState('');

  const onSubmit = () => {
    if (password.length < 8) { alert('비밀번호는 8자 이상이어야 해요.'); return; }
    if (password !== confirm) { alert('비밀번호가 일치하지 않아요.'); return; }
    signup.mutate(
      { username, password, nickname, recoveryEmail: recoveryEmail.trim() || null },
      {
        onSuccess: (r) => { setSession(r.userId, r.token, r.refreshToken); router.push('/onboard'); },
        onError: () => alert('아이디가 이미 사용 중이거나 입력값을 확인해 주세요.'),
      },
    );
  };

  return (
    <main className="mx-auto flex min-h-screen max-w-sm flex-col justify-center px-8">
      <h1 className="text-3xl font-bold text-ink">회원가입</h1>
      <p className="mt-2 text-ink-soft">이메일·휴대폰 인증 없이 바로 시작하세요.</p>

      <div className="mt-8 flex flex-col gap-3">
        <input value={nickname} onChange={(e) => setNickname(e.target.value)} placeholder="닉네임" className={input} />
        <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="아이디" autoCapitalize="none" className={input} />
        <input value={password} onChange={(e) => setPassword(e.target.value)} type="password" placeholder="비밀번호 (8자 이상)" className={input} />
        <input value={confirm} onChange={(e) => setConfirm(e.target.value)} type="password" placeholder="비밀번호 확인" className={input} />
        <input value={recoveryEmail} onChange={(e) => setRecoveryEmail(e.target.value)} type="email" placeholder="복구 이메일 (선택)" autoCapitalize="none" className={input} />
      </div>

      <div className="mt-3 rounded-md bg-cloud p-3.5">
        <p className="text-xs leading-relaxed text-ink-soft">
          복구 이메일을 입력하지 않으면 비밀번호를 잊었을 때 계정을 복구할 수 없어요.
        </p>
      </div>

      <Button className="mt-4 w-full" onClick={onSubmit} disabled={signup.isPending}>
        {signup.isPending ? '가입 중…' : '가입하고 슬롯 만들기'}
      </Button>

      <p className="mt-5 text-center text-sm text-ink-faint">
        이미 계정이 있으신가요?{' '}
        <Link href="/login" className="font-semibold text-pine">로그인</Link>
      </p>
    </main>
  );
}
