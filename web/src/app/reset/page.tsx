'use client';
import { Suspense, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { usePasswordResetConfirm } from '@/lib/hooks';
import { Button } from '@/components/ui/button';

const input = 'h-12 rounded-md border border-input bg-card px-4 text-ink outline-none focus:ring-2 focus:ring-ring';

// SMTP 메일 링크(/reset?token=…)가 착지하는 페이지.
function ResetConfirmInner() {
  const router = useRouter();
  const token = useSearchParams().get('token') ?? '';
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const reset = usePasswordResetConfirm();

  const onSubmit = () => {
    if (password.length < 8) { alert('비밀번호는 8자 이상이어야 해요.'); return; }
    if (password !== confirm) { alert('비밀번호가 일치하지 않아요.'); return; }
    reset.mutate(
      { resetToken: token, newPassword: password },
      {
        onSuccess: () => { alert('비밀번호가 변경됐어요. 다시 로그인해 주세요.'); router.push('/login'); },
        onError: () => alert('링크가 만료되었거나 유효하지 않아요. 다시 요청해 주세요.'),
      },
    );
  };

  if (!token) {
    return (
      <main className="mx-auto flex min-h-screen max-w-sm flex-col justify-center px-8">
        <h1 className="text-2xl font-bold text-ink">유효하지 않은 링크</h1>
        <p className="mt-2 text-ink-soft">재설정 링크가 올바르지 않아요.</p>
        <Link href="/reset/request" className="mt-6 font-semibold text-pine">재설정 다시 요청하기</Link>
      </main>
    );
  }

  return (
    <main className="mx-auto flex min-h-screen max-w-sm flex-col justify-center px-8">
      <h1 className="text-3xl font-bold text-ink">새 비밀번호 설정</h1>
      <div className="mt-8 flex flex-col gap-3">
        <input value={password} onChange={(e) => setPassword(e.target.value)} type="password" placeholder="새 비밀번호 (8자 이상)" className={input} />
        <input value={confirm} onChange={(e) => setConfirm(e.target.value)} type="password" placeholder="새 비밀번호 확인" className={input} />
      </div>
      <Button className="mt-4 w-full" onClick={onSubmit} disabled={reset.isPending}>
        {reset.isPending ? '변경 중…' : '비밀번호 변경'}
      </Button>
    </main>
  );
}

export default function ResetConfirmPage() {
  return (
    <Suspense fallback={null}>
      <ResetConfirmInner />
    </Suspense>
  );
}
