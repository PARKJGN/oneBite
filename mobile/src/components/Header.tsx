import React, { useState } from 'react';
import { Modal, Pressable, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { MenuIcon } from './icons';
import { useSession } from '../store/session';
import { colors } from '../theme';

// 상단 헤더(네비게이터 관리, 전 화면 공유) — 좌측 oneBite(상세에선 뒤로가기 추가), 우측 햄버거 → 드롭다운.
// back=true면 뒤로가기 화살표를 보여준다(스택 push 화면). 디자인(oneBite.dc.html) 기준.
export function Header({ navigation, back = false }: { navigation: any; back?: boolean }) {
  const [open, setOpen] = useState(false);
  const insets = useSafeAreaInsets();
  const clear = useSession((s) => s.clear);

  // 전역 메뉴 이동: 메인 위 화면(상세·다른 메뉴)을 모두 정리하고 대상 하나만 올린다 →
  // 항상 [Main, 대상] 구조라 책갈피↔설정을 오가도 프레임이 쌓이지 않고, 뒤로가기는 늘 메인으로.
  // popTo('Main')은 이름 기준이라 Main이 어느 인덱스든 안전하고, 탭 상태도 보존된다.
  const go = (screen: string) => {
    setOpen(false);
    navigation.popTo('Main');
    navigation.navigate(screen);
  };
  const logout = () => { setOpen(false); clear(); navigation.reset({ index: 0, routes: [{ name: 'Login' }] }); };

  return (
    <View style={{ paddingTop: insets.top, backgroundColor: colors.bg, zIndex: 10 }}>
      <View style={{ height: 44, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 18, borderBottomWidth: 1, borderBottomColor: '#eef2ee' }}>
        <View style={{ flexDirection: 'row', alignItems: 'center', gap: 10 }}>
          {back ? (
            // 뒤로가기 화면: 화살표만(oneBite 워드마크 숨김)
            <Pressable onPress={() => navigation.goBack()} hitSlop={12}>
              <Text style={{ fontSize: 24, color: colors.ink, lineHeight: 26 }}>←</Text>
            </Pressable>
          ) : (
            <Text style={{ fontSize: 18, fontWeight: '700', color: colors.green, letterSpacing: -0.3 }}>oneBite</Text>
          )}
        </View>
        <Pressable onPress={() => setOpen(true)} hitSlop={12}>
          <MenuIcon color={colors.ink} />
        </Pressable>
      </View>

      <Modal visible={open} transparent animationType="fade" onRequestClose={() => setOpen(false)}>
        <Pressable style={{ flex: 1 }} onPress={() => setOpen(false)}>
          <View style={{
            position: 'absolute', top: insets.top + 44, right: 14, width: 168,
            backgroundColor: '#fff', borderRadius: 13, borderWidth: 1, borderColor: '#e2e9e3', padding: 6,
            shadowColor: '#142818', shadowOpacity: 0.16, shadowRadius: 30, shadowOffset: { width: 0, height: 12 }, elevation: 8,
          }}>
            <MenuItem label="내 책갈피" onPress={() => go('Bookmarks')} />
            <Divider />
            <MenuItem label="설정" onPress={() => go('Settings')} />
            <Divider />
            <MenuItem label="로그아웃" color={colors.danger} onPress={logout} />
          </View>
        </Pressable>
      </Modal>
    </View>
  );
}

const MenuItem = ({ label, onPress, color = colors.ink }: { label: string; onPress: () => void; color?: string }) => (
  <Pressable onPress={onPress} style={({ pressed }) => ({ paddingVertical: 11, paddingHorizontal: 14, borderRadius: 9, backgroundColor: pressed ? colors.greenSoft : 'transparent' })}>
    <Text style={{ fontSize: 14, fontWeight: '600', color }}>{label}</Text>
  </Pressable>
);

const Divider = () => <View style={{ height: 1, backgroundColor: '#eef2ee', marginVertical: 4, marginHorizontal: 6 }} />;
