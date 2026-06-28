import { useCallback, useEffect, useRef, useState } from 'react';
import { Animated, Easing } from 'react-native';

/**
 * 한 입 베어무는 인터렉션 훅 — 디자인 eat()과 동일: 560ms easeOutCubic로 scoop 0→1,
 * 중간에 살짝 스케일 펄스(squash), 다 베어물면 onEaten()으로 상세 이동.
 * 이미 읽은(read=true) 에디션은 베어물지 않고 바로 onEaten().
 *
 *  - bite: EditionThumb의 bite prop으로 전달(0~1, scoop 진행도)
 *  - scale: 썸네일을 감싼 Animated.View transform에 연결할 펄스 스케일
 *  - onPress: 카드 Pressable의 onPress에 연결
 */
export function useBite(read: boolean, onEaten: () => void, pulse = 0.05) {
  const prog = useRef(new Animated.Value(read ? 1 : 0)).current;
  const [bite, setBite] = useState(read ? 1 : 0);
  const eating = useRef(false);

  useEffect(() => {
    const id = prog.addListener(({ value }) => setBite(value));
    return () => prog.removeListener(id);
  }, [prog]);

  const onPress = useCallback(() => {
    if (read || eating.current) { onEaten(); return; }
    eating.current = true;
    Animated.timing(prog, {
      toValue: 1,
      duration: 560,
      easing: Easing.out(Easing.cubic),
      useNativeDriver: false, // bite scoop은 JS 리스너로 SVG 마스크를 갱신하므로 네이티브 드라이버 불가
    }).start(() => { eating.current = false; onEaten(); });
  }, [read, onEaten, prog]);

  const scale = prog.interpolate({ inputRange: [0, 0.5, 1], outputRange: [1, 1 - pulse, 1] });
  return { bite, scale, onPress };
}
