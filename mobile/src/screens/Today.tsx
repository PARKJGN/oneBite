import React, { useState } from 'react';
import { Animated, Pressable, ScrollView, Text, View } from 'react-native';
import { useToday } from '../api/today';
import { useYesterdayHighlights } from '../api/home';
import { useCategories } from '../api/slots';
import { EditionThumb } from '../components/EditionThumb';
import { useBite } from '../components/useBite';
import { openArticle } from '../lib/openArticle';
import { colors, radius } from '../theme';

// 슬롯별 "오늘의 한 입" 카드 — 탭하면 베어무는 애니메이션 후 상세로 이동(읽었으면 바로 이동)
function TodaySlotCard({ slot, onOpen }: { slot: any; onOpen: (id: number | null) => void }) {
  const { bite, scale, onPress } = useBite(slot.read, () => onOpen(slot.editionId));
  return (
    <Pressable onPress={onPress}
      style={({ pressed }) => [
        { flexDirection: 'row', gap: 14, alignItems: 'center', padding: 16, backgroundColor: '#fdfcf7', borderWidth: 1, borderColor: '#e6ede8', borderRadius: 14 },
        pressed && { backgroundColor: '#f0f4f0' },
      ]}>
      <Animated.View style={{ transform: [{ scale }] }}>
        <EditionThumb bite={bite} seed={slot.editionId ?? 0} style={{ width: 64, height: 64 }} rounded={12} />
      </Animated.View>
      <View style={{ flex: 1 }}>
        <Text style={{ fontSize: 12, fontWeight: '600', color: colors.green }}>{slot.categoryLine}</Text>
        <Text numberOfLines={2} style={{ fontFamily: 'serif', fontSize: 17, fontWeight: '600', color: colors.ink, marginTop: 6, lineHeight: 23 }}>
          {slot.oneLine ?? '준비 중'}
        </Text>
      </View>
    </Pressable>
  );
}

// 오늘 화면 — 오늘의 한 입(슬롯별, 읽음 라벨 FR-020) + 어제 핵심 뉴스(5개씩 페이징 FR-021)
export default function Today({ navigation }: any) {
  const today = useToday();
  const [page, setPage] = useState(0);
  const yest = useYesterdayHighlights(page);
  const categories = useCategories();
  // 카테고리 코드 → 한글 이름(없으면 코드 그대로)
  const catName = (code: string) => categories.data?.find((c) => c.code === code)?.name ?? code;

  const openEdition = (id: number | null) => { if (id != null) navigation.navigate('EditionDetail', { id }); };
  // 읽을 에디션이 하나라도 있으면 "준비 중" 배너는 숨김(슬롯별 카드가 개별 상태 표시)
  const anyReady = today.data?.slots.some((s) => s.editionId != null) ?? false;

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.bg }} contentContainerStyle={{ padding: 22, paddingTop: 16 }}>
      <View>
        <Text style={{ fontSize: 13, color: '#8a9990' }}>{today.data?.issueDate ?? ''}</Text>
        <Text style={{ fontSize: 30, fontWeight: '700', color: colors.ink, marginTop: 3 }}>오늘의 한 입</Text>
      </View>

      {today.data?.banner && !anyReady ? (
        <View style={{ marginTop: 14, padding: 13, backgroundColor: colors.greenSoft, borderRadius: radius.md }}>
          <Text style={{ color: '#3a5246', fontSize: 13 }}>{today.data.banner}</Text>
        </View>
      ) : null}

      <View style={{ marginTop: 14, gap: 12 }}>
        {today.data?.slots.map((s) => (
          <TodaySlotCard key={s.comboKey} slot={s} onOpen={openEdition} />
        ))}
      </View>

      {/* 어제 핵심 뉴스 — 5개씩 페이지네이션 */}
      <View style={{ marginTop: 26 }}>
        <Text style={{ fontSize: 12, fontWeight: '700', color: colors.green }}>어제의 한입, 놓치셨나요?</Text>
        <Text style={{ fontSize: 20, fontWeight: '700', color: colors.ink, marginTop: 6 }}>어제 핵심 뉴스</Text>

        <View style={{ marginTop: 12, gap: 10 }}>
          {yest.data?.items.map((y, i) => (
            <Pressable key={`${y.editionId}-${i}`} onPress={() => openArticle(y.url)}
              style={({ pressed }) => [
                { flexDirection: 'row', gap: 12, padding: 14, backgroundColor: '#fdfcf7', borderWidth: 1, borderColor: '#e6ede8', borderRadius: 14 },
                pressed && { transform: [{ scale: 0.99 }], backgroundColor: '#f0f4f0' },
              ]}>
              <Text style={{ fontWeight: '600', color: colors.green, width: 20 }}>{page * 5 + i + 1}</Text>
              <View style={{ flex: 1 }}>
                <Text style={{ fontSize: 10, fontWeight: '700', color: colors.green }}>{catName(y.categoryCode)}</Text>
                <Text style={{ fontSize: 15, fontWeight: '600', color: colors.ink, marginTop: 4 }}>{y.title}</Text>
                <Text style={{ fontSize: 12, color: colors.inkFaint, marginTop: 4 }}>{y.source}</Text>
              </View>
            </Pressable>
          ))}
          {(yest.data?.totalItems ?? 0) === 0 && (
            <Text style={{ fontSize: 13, color: colors.inkFaint }}>어제 받은 뉴스가 없어요.</Text>
          )}
        </View>

        {(yest.data?.totalPages ?? 0) > 1 && (
          <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 18, marginTop: 14 }}>
            <Text onPress={() => setPage((p) => Math.max(0, p - 1))} style={{ color: page === 0 ? colors.inkFaint : colors.green, fontSize: 18 }}>‹</Text>
            <Text style={{ fontSize: 13, color: colors.inkSoft }}>{page + 1} / {yest.data!.totalPages}</Text>
            <Text onPress={() => setPage((p) => Math.min((yest.data!.totalPages - 1), p + 1))}
              style={{ color: page >= yest.data!.totalPages - 1 ? colors.inkFaint : colors.green, fontSize: 18 }}>›</Text>
          </View>
        )}
      </View>
    </ScrollView>
  );
}
