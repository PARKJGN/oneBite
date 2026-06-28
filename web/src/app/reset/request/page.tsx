'use client';
import { useState } from 'react';
import Link from 'next/link';
import { usePasswordResetRequest } from '@/lib/hooks';
import { Button } from '@/components/ui/button';

const input = 'h-12 rounded-md border border-input bg-card px-4 text-ink outline-none focus:ring-2 focus:ring-ring';

export default function ResetRequestPage() {
  const [username, setUsername] = useState('');
  const [sent, setSent] = useState(false);
  const reset = usePasswordResetRequest();

  // 존재 여부를 노출하지 않도록 항상 동일 안내(백엔드도 항상 202)
  const onSubmit = () => reset.mutate(username, { onSettled: () => setSent(true) });

  return (
    <main className="mx-auto flex min-h-screen max-w-sm flex-col justify-center px-8">
      <h1 className="text-3xl font-bold text-ink">비밀번호 재설정</h1>

      {sent ? (
        <div className="mt-6 rounded-md bg-cloud p-4">
          <p className="text-sm leading-relaxed text-forest">
            복구 이메일이 등록돼 있다면 재설정 링크를 보냈어요. 메일함을 확인해 주세요.
          </p>
        </div>
      ) : (
        <>
          <p className="mt-2 text-ink-soft">가입 시 등록한 복구 이메일로 재설정 링크를 보내드려요.</p>
          <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="아이디"
            autoCapitalize="none" className={`${input} mt-8`} />
          <Button className="mt-4 w-full" onClick={onSubmit} disabled={reset.isPending || !username}>
            {reset.isPending ? '전송 중…' : '재설정 링크 받기'}
          </Button>
        </>
      )}

      <p className="mt-5 text-center text-sm text-ink-faint">
        <Link href="/login" className="font-semibold text-pine">로그인으로 돌아가기</Link>
      </p>
    </main>
  );
}
