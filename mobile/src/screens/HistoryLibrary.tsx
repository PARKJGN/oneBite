import React from 'react';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { useLibrarySlots } from '../api/library';
import { colors, radius } from '../theme';

// 라이브러리 1단계 — 슬롯 목록(삭제 슬롯도 과거 에디션 있으면 표시). FR-011c
export default function HistoryLibrary({ navigation }: any) {
  const slots = useLibrarySlots();

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.bg }} contentContainerStyle={{ padding: 22, paddingTop: 18 }}>
      <Text style={{ fontSize: 34, fontWeight: '700', color: colors.ink }}>히스토리</Text>
      <Text style={{ fontSize: 13, color: '#7a8b80', marginTop: 5 }}>슬롯별로 받은 에디션을 모았어요.</Text>

      <View style={{ marginTop: 18, gap: 12 }}>
        {slots.data?.map((sl) => (
          <Pressable key={sl.comboKey}
            onPress={() => navigation.navigate('SlotEditions', { comboKey: sl.comboKey, title: sl.categoryLine })}
            style={({ pressed }) => [
              { flexDirection: 'row', alignItems: 'center', gap: 16, padding: 14, backgroundColor: '#fdfcf7', borderWidth: 1, borderColor: '#e6ede8', borderRadius: 14 },
              pressed && { transform: [{ scale: 0.99 }], backgroundColor: '#f0f4f0' },
            ]}>
            <View style={{ flex: 1 }}>
              <Text style={{ fontSize: 18, fontWeight: '600', color: colors.ink }}>
                {sl.categoryLine}{!sl.active ? '  · (보관됨)' : ''}
              </Text>
              <Text style={{ fontSize: 12, color: colors.inkFaint, marginTop: 8 }}>
                {sl.editionCount}개 에디션 받음{sl.latestDate ? ` · 최근 ${sl.latestDate}` : ''}
              </Text>
            </View>
            <Text style={{ fontSize: 20, color: '#c2cfc6' }}>›</Text>
          </Pressable>
        ))}
        {slots.data?.length === 0 && (
          <Text style={{ fontSize: 13, color: colors.inkFaint, marginTop: 10 }}>아직 받은 에디션이 없어요.</Text>
        )}
      </View>
    </ScrollView>
  );
}
