import React from 'react';
import { Alert, ScrollView, Text, TouchableOpacity, View } from 'react-native';
import { useSlots, useDeleteSlot } from '../api/slots';
import { colors, radius } from '../theme';

// 내 슬롯(활성) — 수정(온보딩 재사용)·삭제(소프트 삭제, 라이브러리 유지 FR-003b)·추가
export default function Slots({ navigation }: any) {
  const slots = useSlots();
  const del = useDeleteSlot();
  const count = slots.data?.length ?? 0;

  const onDelete = (id: number) => {
    Alert.alert('슬롯 삭제', '발송만 중단되고, 받은 에디션은 히스토리에 남아요.', [
      { text: '취소', style: 'cancel' },
      { text: '삭제', style: 'destructive', onPress: () => del.mutate(id) },
    ]);
  };

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.bg }} contentContainerStyle={{ padding: 22, paddingTop: 18 }}>
      <Text style={{ fontSize: 28, fontWeight: '700', color: colors.ink }}>내 슬롯</Text>
      <Text style={{ fontSize: 13, color: colors.inkSoft, marginTop: 8 }}>변경한 내용은 다음 오전 8시 발송부터 반영돼요.</Text>
      <Text style={{ fontSize: 12, fontWeight: '600', color: colors.inkFaint, marginTop: 16 }}>{count} / 3 슬롯 사용 중</Text>

      <View style={{ marginTop: 12 }}>
        {slots.data?.map((s) => (
          <View key={s.id} style={{ paddingVertical: 16, borderTopWidth: 1, borderTopColor: '#e8ede9', flexDirection: 'row', alignItems: 'center' }}>
            <Text style={{ flex: 1, fontSize: 17, fontWeight: '600', color: colors.ink }}>{s.categoryLine}</Text>
            <Text onPress={() => navigation.navigate('Onboard', { slotId: s.id, initial: s.categoryCodes })} style={{ fontSize: 13, fontWeight: '600', color: colors.green, marginRight: 16 }}>수정</Text>
            <Text onPress={() => onDelete(s.id)} style={{ fontSize: 13, fontWeight: '600', color: colors.danger }}>삭제</Text>
          </View>
        ))}
      </View>

      {count < 3 && (
        <TouchableOpacity onPress={() => navigation.navigate('Onboard')}
          style={{ marginTop: 22, padding: 15, backgroundColor: colors.greenSoft, borderWidth: 1, borderColor: '#aecbb8', borderStyle: 'dashed', borderRadius: radius.md, alignItems: 'center' }}>
          <Text style={{ fontSize: 15, fontWeight: '600', color: colors.green }}>+ 슬롯 추가</Text>
        </TouchableOpacity>
      )}
    </ScrollView>
  );
}
