import Link from 'next/link';
import { CONTACT_EMAIL, EFFECTIVE_DATE } from '@/lib/legal';

/**
 * 법적 고지 문서 공용 셸.
 *
 * 이 페이지들은 스토어 심사자·크롤러가 "비로그인" 상태로 방문하므로 세션에 의존하는 요소를
 * 두지 않는다. (AppChrome 의 NO_NAV 에 등록되어 헤더/탭바도 렌더되지 않는다.)
 */

/** 본문 태그에 에디토리얼 타이포를 입히는 래퍼 — 별도 prose 플러그인 없이 처리. */
const PROSE = [
  'mt-8 text-[15px] leading-7 text-ink-soft',
  '[&_h2]:mt-10 [&_h2]:text-lg [&_h2]:font-bold [&_h2]:text-ink',
  '[&_h3]:mt-6 [&_h3]:text-[15px] [&_h3]:font-semibold [&_h3]:text-ink',
  '[&_p]:mt-3',
  '[&_ul]:mt-3 [&_ul]:list-disc [&_ul]:space-y-1.5 [&_ul]:pl-5',
  '[&_strong]:font-semibold [&_strong]:text-ink',
  '[&_a]:font-semibold [&_a]:text-pine [&_a]:underline',
].join(' ');

export function LegalPage({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <main className="container max-w-2xl py-10">
      <Link href="/" className="text-sm font-semibold text-pine">← oneBite</Link>

      <h1 className="mt-4 text-2xl font-bold text-ink">{title}</h1>
      <p className="mt-2 text-xs font-semibold text-ink-faint">시행일 {EFFECTIVE_DATE}</p>

      <div className={PROSE}>{children}</div>

      <p className="mt-12 border-t border-border pt-6 text-xs text-ink-faint">
        문의 · <a href={`mailto:${CONTACT_EMAIL}`} className="font-semibold text-pine underline">{CONTACT_EMAIL}</a>
      </p>
    </main>
  );
}
