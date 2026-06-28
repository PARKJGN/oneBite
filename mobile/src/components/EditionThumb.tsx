import React from 'react';
import { View, ViewStyle } from 'react-native';
import Svg, { Defs, LinearGradient, Stop, Mask, G, Rect, Circle, Path, Ellipse } from 'react-native-svg';
import { colors } from '../theme';

/**
 * 에디션 썸네일 — 브랜드풍 미니멀 "공간 이미지"(SVG 벡터) + "한 입 베어문" 인터렉션.
 * 따뜻한 하늘 + 해 + 겹친 언덕 + 새싹으로 oneBite의 차분한 톤을 표현한다(래스터 이미지 대체).
 *
 * 한입(biteScoop): 디자인과 동일하게 우상단 모서리를 '마스크로 도려낸다'.
 *   - 큰 스쿱(R) + 위쪽 모서리 왼쪽의 작은 스쿱(R×0.55)이 겹쳐 "베어문 가리비" 가장자리를 만든다.
 *   - 도려낸 자리는 투명 → 뒤의 카드 배경이 비친다. + 가장자리에 진한 호(rim)를 덧그려 어느 배경에서도 또렷하게.
 *   - bite=0이면 멀쩡, bite=1이면 한 입 베어문 상태. read는 곧 bite=1.
 *  - seed: 에디션마다 팔레트/해 위치를 살짝 달리해 단조로움을 줄인다(같은 에디션은 항상 동일).
 *
 * preserveAspectRatio는 xMidYMin(위쪽 정렬) — 가로로 긴 썸네일에서도 위쪽 모서리(베어문 자국)가 잘리지 않게.
 */
const PALETTES = [
  { sky0: '#fdf6e9', sky1: '#eef4ef', sun: colors.clay, hillBack: '#cfe0d2', hillFront: colors.green, sprout: '#3f7d5e' },
  { sky0: '#f3f7f1', sky1: '#e3eee6', sun: '#e0b878', hillBack: '#c2d8c7', hillFront: '#2c5f47', sprout: '#3f7d5e' },
  { sky0: '#fbf1e6', sky1: '#eaf1ea', sun: '#d9a35f', hillBack: '#d3e1d4', hillFront: '#357154', sprout: '#4a8a68' },
  { sky0: '#f6f4ec', sky1: '#e6efe7', sun: colors.clay, hillBack: '#c8dccd', hillFront: '#2f6b4f', sprout: '#3f7d5e' },
];

export function EditionThumb({
  read = false,
  bite,
  style,
  rounded = 13,
  seed = 0,
}: { read?: boolean; bite?: number; style?: ViewStyle; rounded?: number; cutColor?: string; seed?: number }) {
  const p = PALETTES[Math.abs(seed) % PALETTES.length];
  const sunCx = 70 + (Math.abs(seed) % 3) * 6; // 64~76: 해 위치 약간씩 이동
  const t = Math.max(0, Math.min(1, bite ?? (read ? 1 : 0))); // 베어문 정도 0~1
  // viewBox 100×100 기준 스쿱 반경(디자인 biteScoop: R=8+21p, R2=R*0.55, c2x=100%-R*0.72)
  const R = 9 + 27 * t;
  const R2 = R * 0.55;
  const c2x = 100 - R * 0.72;
  // 베어문 가장자리 = 두 원 합집합의 바깥 윤곽. 잡선 없이 외곽선만 샘플링해 단일 Path로(rim용).
  const rimPath = (() => {
    if (t <= 0) return '';
    const xL = 100 - 1.27 * R; // 합집합 최좌측(작은 원 왼쪽 끝)
    const pts: string[] = [];
    const N = 28;
    for (let i = 0; i <= N; i++) {
      const x = xL + (100 - xL) * (i / N);
      const ys = Math.abs(x - c2x) <= R2 ? Math.sqrt(R2 * R2 - (x - c2x) ** 2) : -1;
      const yb = Math.abs(x - 100) <= R ? Math.sqrt(R * R - (x - 100) ** 2) : -1;
      const y = Math.max(ys, yb, 0);
      pts.push(`${x.toFixed(2)} ${y.toFixed(2)}`);
    }
    return 'M' + pts.join(' L');
  })();

  return (
    <View style={[{ backgroundColor: 'transparent', overflow: 'hidden', borderRadius: rounded }, style]}>
      <Svg width="100%" height="100%" viewBox="0 0 100 100" preserveAspectRatio="xMidYMin slice">
        <Defs>
          <LinearGradient id="sky" x1="0" y1="0" x2="0" y2="1">
            <Stop offset="0" stopColor={p.sky0} />
            <Stop offset="1" stopColor={p.sky1} />
          </LinearGradient>
          {/* 흰색=보임, 검정=도려냄 → 우상단을 베어문다 */}
          <Mask id="bite" x="0" y="0" width="100" height="100">
            <Rect x="0" y="0" width="100" height="100" fill="#fff" />
            {t > 0 && (
              <>
                <Circle cx="100" cy="0" r={R} fill="#000" />
                <Circle cx={c2x} cy="0" r={R2} fill="#000" />
              </>
            )}
          </Mask>
        </Defs>
        <G mask="url(#bite)">
          {/* 하늘 */}
          <Rect x="0" y="0" width="100" height="100" fill="url(#sky)" />
          {/* 해 */}
          <Circle cx={sunCx} cy="30" r="13" fill={p.sun} opacity={0.92} />
          {/* 뒤쪽 언덕 */}
          <Path d="M0 70 Q30 52 60 64 T100 60 V100 H0 Z" fill={p.hillBack} />
          {/* 앞쪽 언덕 */}
          <Path d="M0 84 Q26 68 52 80 T100 76 V100 H0 Z" fill={p.hillFront} />
          {/* 새싹 — 줄기 + 두 잎 */}
          <Path d="M26 84 V72" stroke={p.sprout} strokeWidth="2.4" strokeLinecap="round" fill="none" />
          <Ellipse cx="21.5" cy="71" rx="5" ry="3" fill={p.sprout} transform="rotate(-28 21.5 71)" />
          <Ellipse cx="30.5" cy="69" rx="5" ry="3" fill={p.sprout} transform="rotate(28 30.5 69)" />
        </G>
        {/* 베어문 가장자리 테두리(rim) — 카드 테두리와 동일한 색/굵기로 자연스럽게 이어지게 */}
        {t > 0 && <Path d={rimPath} fill="none" stroke="#e6ede8" strokeWidth="1.2" strokeLinejoin="round" strokeLinecap="round" />}
      </Svg>
    </View>
  );
}
