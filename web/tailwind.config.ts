import type { Config } from 'tailwindcss';

// oneBite 디자인 토큰(포레스트 그린 에디토리얼) — 디자인 시스템 단일 출처.
const config: Config = {
  darkMode: ['class'],
  content: ['./src/**/*.{ts,tsx}'],
  theme: {
    container: { center: true, padding: '1.5rem', screens: { '2xl': '1056px' } },
    extend: {
      colors: {
        // 브랜드 그린 — 표면 위계
        deepforest: '#14301f', // 다크 밴드·몰입 헤더
        forest: '#1f4d39', // 피처 밴드·다크 서피스
        pine: '#2f6b4f', // PRIMARY · CTA · 선택
        sage: '#3f7d5f', // 다크 위 악센트
        // 서피스 & 틴트
        canvas: '#ece8e0', // 웜 캔버스(페이지 배경)
        paper: '#f7f9f6',
        cloud: '#eef4ef', // 민트 틴트
        // 악센트 · 텍스트 · 세만틱
        clay: '#c79a5e', // ceremony(책갈피·완료) 한정
        ink: '#16241c', // 제목
        'ink-soft': '#5c6b62', // 본문/보조
        'ink-faint': '#9aa89f',
        danger: '#b4493d',
        // shadcn 시맨틱 매핑(컴포넌트가 bg-background 등 사용)
        background: '#f7f9f6',
        foreground: '#16241c',
        primary: { DEFAULT: '#2f6b4f', foreground: '#ffffff' },
        secondary: { DEFAULT: '#eef4ef', foreground: '#2f6b4f' },
        muted: { DEFAULT: '#eef4ef', foreground: '#5c6b62' },
        accent: { DEFAULT: '#c79a5e', foreground: '#ffffff' },
        destructive: { DEFAULT: '#b4493d', foreground: '#ffffff' },
        border: '#e6ede8',
        input: '#d4e0d7',
        ring: '#2f6b4f',
        card: { DEFAULT: '#ffffff', foreground: '#16241c' },
      },
      borderRadius: { lg: '14px', md: '12px', sm: '9px', pill: '999px' },
      fontFamily: {
        sans: ['"Pretendard Variable"', 'Pretendard', 'system-ui', 'sans-serif'],
        serif: ['"Noto Serif KR"', 'serif'], // 제목·에디토리얼
      },
      keyframes: {
        'fade-up': { from: { opacity: '0', transform: 'translateY(10px)' }, to: { opacity: '1', transform: 'none' } },
      },
      animation: { 'fade-up': 'fade-up .34s cubic-bezier(.2,.7,.2,1) both' },
    },
  },
  plugins: [require('tailwindcss-animate')],
};

export default config;
