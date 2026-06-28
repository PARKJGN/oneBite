'use client';
import { useId } from 'react';

/**
 * 에디션 썸네일(웹) — 모바일과 동일한 브랜드풍 "공간 이미지"(SVG) + "한 입 베어문" 마스크.
 * 우상단 모서리를 두 원 합집합으로 도려내고(투명), 합집합 외곽선(rim)을 카드 테두리색으로 그린다.
 *  - bite=0 멀쩡 / bite=1 한 입 베어문. read면 곧 bite=1.
 *  - seed: 에디션마다 팔레트/해 위치를 살짝 달리(같은 에디션은 항상 동일).
 */
const PALETTES = [
  { sky0: '#fdf6e9', sky1: '#eef4ef', sun: '#c79a5e', hillBack: '#cfe0d2', hillFront: '#2f6b4f', sprout: '#3f7d5e' },
  { sky0: '#f3f7f1', sky1: '#e3eee6', sun: '#e0b878', hillBack: '#c2d8c7', hillFront: '#2c5f47', sprout: '#3f7d5e' },
  { sky0: '#fbf1e6', sky1: '#eaf1ea', sun: '#d9a35f', hillBack: '#d3e1d4', hillFront: '#357154', sprout: '#4a8a68' },
  { sky0: '#f6f4ec', sky1: '#e6efe7', sun: '#c79a5e', hillBack: '#c8dccd', hillFront: '#2f6b4f', sprout: '#3f7d5e' },
];

export function EditionThumb({
  read = false,
  bite,
  seed = 0,
  className,
}: { read?: boolean; bite?: number; seed?: number; className?: string }) {
  const uid = useId().replace(/[:]/g, ''); // SVG id 충돌 방지(인스턴스별 고유)
  const p = PALETTES[Math.abs(seed) % PALETTES.length];
  const sunCx = 70 + (Math.abs(seed) % 3) * 6;
  const t = Math.max(0, Math.min(1, bite ?? (read ? 1 : 0)));
  const R = 9 + 27 * t;
  const R2 = R * 0.55;
  const c2x = 100 - R * 0.72;

  // 두 원 합집합의 바깥 외곽선만 샘플링(잡선 없이 베어문 곡선)
  let rimPath = '';
  if (t > 0) {
    const xL = 100 - 1.27 * R;
    const pts: string[] = [];
    const N = 28;
    for (let i = 0; i <= N; i++) {
      const x = xL + (100 - xL) * (i / N);
      const ys = Math.abs(x - c2x) <= R2 ? Math.sqrt(R2 * R2 - (x - c2x) ** 2) : -1;
      const yb = Math.abs(x - 100) <= R ? Math.sqrt(R * R - (x - 100) ** 2) : -1;
      const y = Math.max(ys, yb, 0);
      pts.push(`${x.toFixed(2)} ${y.toFixed(2)}`);
    }
    rimPath = 'M' + pts.join(' L');
  }

  return (
    <svg
      className={className}
      viewBox="0 0 100 100"
      preserveAspectRatio="xMidYMin slice"
      style={{ display: 'block', width: '100%', height: '100%', background: p.sky1 }}
    >
      <defs>
        <linearGradient id={`sky${uid}`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor={p.sky0} />
          <stop offset="1" stopColor={p.sky1} />
        </linearGradient>
        <mask id={`bite${uid}`}>
          <rect x="0" y="0" width="100" height="100" fill="#fff" />
          {t > 0 && (
            <>
              <circle cx="100" cy="0" r={R} fill="#000" />
              <circle cx={c2x} cy="0" r={R2} fill="#000" />
            </>
          )}
        </mask>
      </defs>
      <g mask={`url(#bite${uid})`}>
        <rect x="0" y="0" width="100" height="100" fill={`url(#sky${uid})`} />
        <circle cx={sunCx} cy="30" r="13" fill={p.sun} opacity={0.92} />
        <path d="M0 70 Q30 52 60 64 T100 60 V100 H0 Z" fill={p.hillBack} />
        <path d="M0 84 Q26 68 52 80 T100 76 V100 H0 Z" fill={p.hillFront} />
        <path d="M26 84 V72" stroke={p.sprout} strokeWidth="2.4" strokeLinecap="round" fill="none" />
        <ellipse cx="21.5" cy="71" rx="5" ry="3" fill={p.sprout} transform="rotate(-28 21.5 71)" />
        <ellipse cx="30.5" cy="69" rx="5" ry="3" fill={p.sprout} transform="rotate(28 30.5 69)" />
      </g>
      {t > 0 && (
        <path d={rimPath} fill="none" stroke="#e6ede8" strokeWidth="1.2" strokeLinejoin="round" strokeLinecap="round" />
      )}
    </svg>
  );
}
