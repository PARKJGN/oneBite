import React from 'react';
import Svg, { Path, Rect } from 'react-native-svg';

// 디자인(oneBite.dc.html)의 SVG 경로를 그대로 사용 — 하단 네비 3종 + 햄버거 + 책갈피
type IconProps = { color: string; size?: number };

export const HomeIcon = ({ color, size = 20 }: IconProps) => (
  <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
    <Path d="M4 11l8-7 8 7v8a1 1 0 0 1-1 1h-4v-6h-6v6H5a1 1 0 0 1-1-1z" stroke={color} strokeWidth={1.7} strokeLinejoin="round" />
  </Svg>
);

export const HistoryIcon = ({ color, size = 20 }: IconProps) => (
  <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
    <Path d="M5 4h5v16H5zM14 4h5v16h-5z" stroke={color} strokeWidth={1.7} strokeLinejoin="round" />
    <Path d="M7 8h1M16 8h1" stroke={color} strokeWidth={1.7} strokeLinecap="round" />
  </Svg>
);

export const SlotsIcon = ({ color, size = 20 }: IconProps) => (
  <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
    <Rect x={4} y={5} width={16} height={14} rx={2} stroke={color} strokeWidth={1.7} />
    <Path d="M4 9h16" stroke={color} strokeWidth={1.7} />
  </Svg>
);

export const MenuIcon = ({ color, size = 22 }: IconProps) => (
  <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
    <Path d="M4 7h16M4 12h16M4 17h16" stroke={color} strokeWidth={2} strokeLinecap="round" />
  </Svg>
);

// 책갈피 — bookmarked면 채움(Clay Gold ceremony 색)
export const BookmarkIcon = ({ color, size = 20, filled = false }: IconProps & { filled?: boolean }) => (
  <Svg width={size} height={size} viewBox="0 0 24 24" fill={filled ? color : 'none'}>
    <Path d="M6 4h12v16l-6-4-6 4z" stroke={color} strokeWidth={1.7} strokeLinejoin="round" />
  </Svg>
);
