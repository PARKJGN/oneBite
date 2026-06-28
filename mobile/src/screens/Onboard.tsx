import React, { useState } from 'react';
import { Alert, ScrollView, Text, TouchableOpacity, View } from 'react-native';
import { useCategories, useCreateSlot, useUpdateSlot } from '../api/slots';
import { requestPushPermission } from '../lib/permission';
import { syncTimezone } from '../lib/timezone';
import { colors, radius } from '../theme';

const MAX = 4; // 슬롯당 카테고리 최대 4개(FR-003)

export default function Onboard({ navigation, route }: any) {
  const editSlotId: number | undefined = route?.params?.slotId;
  const initial: string[] = route?.params?.initial ?? [];
  const isEdit = editSlotId != null;

  const categories = useCategories();
  const createSlot = useCreateSlot();
  const updateSlot = useUpdateSlot();
  const [selected, setSelected] = useState<string[]>(initial);

  const toggle = (code: string) => {
    setSelected((prev) =>
      prev.includes(code) ? prev.filter((c) => c !== code) : prev.length < MAX ? [...prev, code] : prev,
    );
  };

  const pending = createSlot.isPending || updateSlot.isPending;

  const onSave = () => {
    if (selected.length === 0) {
      Alert.alert('안내', '카테고리를 하나 이상 선택해 주세요.');
      return;
    }
    if (isEdit) {
      updateSlot.mutate(
        { slotId: editSlotId!, categoryCodes: selected },
        { onSuccess: () => navigation.goBack(), onError: () => Alert.alert('수정 실패', '잠시 후 다시 시도해 주세요.') },
      );
      return;
    }
    createSlot.mutate(selected, {
      onSuccess: async () => {
        await syncTimezone();          // 기기 타임존 저장(T036a)
        await requestPushPermission();  // 권한 요청은 필요한 이 시점에(원칙 III)
        navigation.replace('Main');
      },
      onError: () => Alert.alert('저장 실패', '잠시 후 다시 시도해 주세요.'),
    });
  };

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.bg }} contentContainerStyle={{ padding: 24, paddingTop: 40 }}>
      <Text style={{ fontSize: 12, fontWeight: '700', letterSpacing: 1, color: colors.green }}>슬롯 만들기</Text>
      <Text style={{ fontSize: 25, fontWeight: '600', color: colors.ink, marginTop: 10, lineHeight: 34 }}>
        관심 카테고리를 묶어{'\n'}슬롯을 만드세요
      </Text>
      <Text style={{ fontSize: 14, color: colors.inkSoft, marginTop: 12, lineHeight: 22 }}>
        슬롯은 최대 3개, 슬롯당 카테고리는 최대 4개까지 고를 수 있어요.
      </Text>

      <View style={{ marginTop: 28, flexDirection: 'row', justifyContent: 'space-between' }}>
        <Text style={{ fontSize: 12, fontWeight: '700', color: '#7a8b80' }}>카테고리</Text>
        <Text style={{ fontSize: 12, fontWeight: '600', color: colors.green }}>{selected.length} / {MAX} 선택됨</Text>
      </View>

      <View style={{ marginTop: 12, flexDirection: 'row', flexWrap: 'wrap', gap: 9 }}>
        {categories.data?.map((c) => {
          const on = selected.includes(c.code);
          return (
            <TouchableOpacity key={c.code} onPress={() => toggle(c.code)}
              style={{
                paddingVertical: 9, paddingHorizontal: 15, borderRadius: radius.pill,
                backgroundColor: on ? colors.green : colors.surface,
                borderWidth: 1, borderColor: on ? colors.green : colors.border,
              }}>
              <Text style={{ fontSize: 14, fontWeight: '500', color: on ? '#fff' : '#3a4f43' }}>{c.name}</Text>
            </TouchableOpacity>
          );
        })}
      </View>

      <TouchableOpacity onPress={onSave} disabled={pending}
        style={{ marginTop: 28, backgroundColor: colors.green, borderRadius: radius.md, padding: 16, alignItems: 'center' }}>
        <Text style={{ color: '#fff', fontSize: 16, fontWeight: '600' }}>
          {pending ? '저장 중…' : isEdit ? '변경 저장' : '이 슬롯 저장하고 시작'}
        </Text>
      </TouchableOpacity>

      {/* 알림 권한 안내(원칙 III) */}
      <View style={{ marginTop: 18, padding: 16, backgroundColor: colors.greenSoft, borderRadius: radius.md }}>
        <Text style={{ fontSize: 13, fontWeight: '600', color: colors.green }}>
          알림을 켜면 매일 오전 8시에 묶음 요약을 보내드려요
        </Text>
        <Text style={{ fontSize: 12.5, color: colors.inkSoft, lineHeight: 18, marginTop: 6 }}>
          지금 거부해도 앱은 계속 쓸 수 있고, 나중에 설정에서 바꿀 수 있어요.
        </Text>
      </View>
    </ScrollView>
  );
}
