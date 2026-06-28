import React from 'react';
import { Pressable, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import type { BottomTabBarProps } from '@react-navigation/bottom-tabs';
import { HistoryIcon, HomeIcon, SlotsIcon } from './icons';
import { colors } from '../theme';

const META: Record<string, { label: string; Icon: (p: { color: string; size?: number }) => React.JSX.Element }> = {
  Home: { label: '오늘', Icon: HomeIcon },
  History: { label: '히스토리', Icon: HistoryIcon },
  Slots: { label: '내 슬롯', Icon: SlotsIcon },
};

// 떠 있는 둥근 알약형 하단 네비(디자인: border-radius 31, 그림자, space-around).
export function BottomTabBar({ state, navigation }: BottomTabBarProps) {
  const insets = useSafeAreaInsets();
  return (
    <View style={{ paddingHorizontal: 16, paddingBottom: (insets.bottom || 8) + 14, paddingTop: 8, backgroundColor: colors.bg }}>
      <View style={{
        height: 62, borderRadius: 31, backgroundColor: '#fdfcf7', borderWidth: 1, borderColor: '#e6ede8',
        flexDirection: 'row', alignItems: 'center', justifyContent: 'space-around', paddingHorizontal: 8,
        shadowColor: '#142818', shadowOpacity: 0.1, shadowRadius: 20, shadowOffset: { width: 0, height: 6 }, elevation: 6,
      }}>
        {state.routes.map((route, i) => {
          const meta = META[route.name];
          if (!meta) return null;
          const focused = state.index === i;
          const color = focused ? colors.green : colors.inkFaint;
          return (
            <Pressable
              key={route.key}
              onPress={() => {
                const event = navigation.emit({ type: 'tabPress', target: route.key, canPreventDefault: true });
                if (!focused && !event.defaultPrevented) navigation.navigate(route.name);
              }}
              style={{ alignItems: 'center', justifyContent: 'center', gap: 4, paddingVertical: 6, flex: 1 }}>
              <meta.Icon color={color} />
              <Text style={{ fontSize: 10, fontWeight: '600', color }}>{meta.label}</Text>
            </Pressable>
          );
        })}
      </View>
    </View>
  );
}
