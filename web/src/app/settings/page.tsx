'use client';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useMe, useUpdateLanguage, useDeleteAccount } from '@/lib/hooks';
import { useSession } from '@/store/session';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { cn } from '@/lib/utils';

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="mt-6">
      <p className="text-xs font-bold tracking-wide text-ink-faint">{title}</p>
      <Card className="mt-2"><CardContent className="p-0 [&>*]:border-b [&>*]:border-border [&>*:last-child]:border-0">{children}</CardContent></Card>
    </section>
  );
}

export default function SettingsPage() {
  const router = useRouter();
  const me = useMe();
  const language = useSession((s) => s.language);
  const setLanguage = useSession((s) => s.setLanguage);
  const clear = useSession((s) => s.clear);
  const updateLang = useUpdateLanguage();
  const del = useDeleteAccount();

  const onLanguage = (l: 'ko' | 'en') => { setLanguage(l); updateLang.mutate(l); };
  const onLogout = () => { clear(); router.push('/login'); };
  const onDelete = () => {
    if (confirm('개인정보·슬롯·읽음/책갈피가 삭제됩니다. (받은 뉴스 요약은 익명 보존) 탈퇴할까요?')) {
      del.mutate(undefined, { onSuccess: () => { clear(); router.push('/login'); } });
    }
  };

  // me 로딩 중엔 인라인 스켈레톤, 로드되면 값(없으면 fallback)
  const val = (v: string | null | undefined, fallback: string) =>
    me.isLoading ? <Skeleton className="ml-1 inline-block h-4 w-28 align-middle" /> : (v ?? fallback);

  return (
    <>
      <main className="container max-w-2xl py-10">
        <h1 className="text-2xl font-bold text-ink">설정</h1>

        <Section title="계정">
          <div className="p-4 text-ink">닉네임 · {val(me.data?.nickname, '—')}</div>
          <div className="p-4 text-ink">아이디 · {val(me.data?.username, '소셜 계정')}</div>
          <div className="p-4 text-ink">복구 이메일 · {val(me.data?.recoveryEmail, '미설정')}</div>
        </Section>

        <Section title="언어">
          <div className="flex gap-2 p-3">
            {(['ko', 'en'] as const).map((l) => (
              <button key={l} onClick={() => onLanguage(l)}
                className={cn('flex-1 rounded-pill py-2.5 text-sm font-semibold',
                  language === l ? 'bg-pine text-white' : 'bg-cloud text-pine')}>
                {l === 'ko' ? '한국어' : 'English'}
              </button>
            ))}
          </div>
        </Section>

        <Section title="알림">
          <div className="p-4 text-ink">푸시 권한 · {val(me.data?.pushPermission, 'unknown')}</div>
        </Section>

        <Section title="정보">
          <div className="p-4 text-ink-soft">버전 1.0.0</div>
          <Link href="/terms" className="block p-4 font-semibold text-ink">이용약관</Link>
          <Link href="/privacy" className="block p-4 font-semibold text-ink">개인정보처리방침</Link>
        </Section>

        <Section title="위험 구역">
          <button onClick={onLogout} className="w-full p-4 text-left font-semibold text-ink">로그아웃</button>
          <button onClick={onDelete} className="w-full p-4 text-left font-semibold text-danger">회원 탈퇴</button>
        </Section>
      </main>
    </>
  );
}
