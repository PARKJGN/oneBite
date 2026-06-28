'use client';
import { useCallback, useRef, useState } from 'react';
import { useRouter } from 'next/navigation';
import { animate } from 'framer-motion';

/**
 * 한 입 베어무는 인터랙션(웹) — 모바일 eat()과 동일: 560ms easeOutCubic로 scoop 0→1,
 * 중간 스케일 펄스(squash), 다 베어물면 상세로 이동. 읽은 에디션은 바로 이동.
 *  - bite: EditionThumb 의 bite prop
 *  - scale: 썸네일 wrapper transform: scale()
 *  - onClick: 카드 클릭 핸들러
 */
export function useEat(read: boolean, editionId: number | null, pulse = 0.05) {
  const router = useRouter();
  const [bite, setBite] = useState(read ? 1 : 0);
  const [scale, setScale] = useState(1);
  const eating = useRef(false);

  const onClick = useCallback(() => {
    if (editionId == null) return;
    const go = () => router.push(`/editions/${editionId}`);
    if (read || eating.current) { go(); return; }
    eating.current = true;
    animate(0, 1, {
      duration: 0.56,
      ease: [0.33, 1, 0.68, 1], // easeOutCubic
      onUpdate: (v) => { setBite(v); setScale(1 - pulse * Math.sin(v * Math.PI)); },
      onComplete: () => { eating.current = false; setScale(1); go(); },
    });
  }, [read, editionId, pulse, router]);

  return { bite, scale, onClick };
}
