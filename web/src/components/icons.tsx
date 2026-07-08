// 디자인 시스템(oneBite.dc.html)의 SVG 경로 — 모바일 앱(mobile/src/components/icons.tsx)과 동일.
// currentColor 를 써서 부모의 text-* 색(text-pine / text-ink-faint 등)을 그대로 따른다.
type IconProps = { size?: number; className?: string };

export const HomeIcon = ({ size = 22, className }: IconProps) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" className={className} aria-hidden>
    <path d="M4 11l8-7 8 7v8a1 1 0 0 1-1 1h-4v-6h-6v6H5a1 1 0 0 1-1-1z" stroke="currentColor" strokeWidth={1.7} strokeLinejoin="round" />
  </svg>
);

export const HistoryIcon = ({ size = 22, className }: IconProps) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" className={className} aria-hidden>
    <path d="M5 4h5v16H5zM14 4h5v16h-5z" stroke="currentColor" strokeWidth={1.7} strokeLinejoin="round" />
    <path d="M7 8h1M16 8h1" stroke="currentColor" strokeWidth={1.7} strokeLinecap="round" />
  </svg>
);

export const SlotsIcon = ({ size = 22, className }: IconProps) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" className={className} aria-hidden>
    <rect x={4} y={5} width={16} height={14} rx={2} stroke="currentColor" strokeWidth={1.7} />
    <path d="M4 9h16" stroke="currentColor" strokeWidth={1.7} />
  </svg>
);

export const MenuIcon = ({ size = 22, className }: IconProps) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" className={className} aria-hidden>
    <path d="M4 7h16M4 12h16M4 17h16" stroke="currentColor" strokeWidth={2} strokeLinecap="round" />
  </svg>
);
