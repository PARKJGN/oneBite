import React from 'react';
import { Animated, Pressable, ScrollView, Text, View } from 'react-native';
import { useLibraryEditions } from '../api/library';
import { EditionThumb } from '../components/EditionThumb';
import { useBite } from '../components/useBite';
import { colors } from '../theme';

// 에디션 카드 — 탭하면 베어무는 애니메이션 후 상세로 이동(읽었으면 바로 이동)
function EditionCard({ ed, onOpen }: { ed: any; onOpen: (id: number) => void }) {
  const { bite, scale, onPress } = useBite(ed.read, () => onOpen(ed.editionId), 0.045);
  return (
    <Pressable onPress={onPress}
      style={({ pressed }) => [
        { width: '48%', marginBottom: 18, borderWidth: 1, borderColor: '#e6ede8', borderRadius: 12, overflow: 'hidden', backgroundColor: '#fff' },
        pressed && { backgroundColor: '#f0f4f0' },
      ]}>
      <Animated.View style={{ transform: [{ scale }] }}>
        <EditionThumb bite={bite} seed={ed.editionId} style={{ height: 96 }} rounded={0} />
      </Animated.View>
      <View style={{ padding: 10 }}>
        <Text numberOfLines={2} style={{ fontSize: 13, fontWeight: '600', color: colors.ink }}>{ed.oneLine}</Text>
        <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 8 }}>
          <Text style={{ fontSize: 12, color: colors.inkFaint }}>{ed.issueDate}</Text>
          {bite < 1 && <Text style={{ fontSize: 11, fontWeight: '700', color: colors.green }}>새 에디션</Text>}
        </View>
      </View>
    </Pressable>
  );
}

// 라이브러리 2단계 — 해당 슬롯(조합)이 받은 에디션 그리드. 읽음 상태 노출(FR-020).
export default function SlotEditions({ route, navigation }: any) {
  const { comboKey, title } = route.params;
  const editions = useLibraryEditions(comboKey);
  const openEdition = (id: number) => navigation.navigate('EditionDetail', { id });

  return (
    <ScrollView style={{ flex: 1, backgroundColor: colors.bg }} contentContainerStyle={{ padding: 22, paddingTop: 18 }}>
      <Text style={{ fontSize: 25, fontWeight: '700', color: colors.ink }}>{title}</Text>
      <Text style={{ fontSize: 12, color: colors.inkFaint, marginTop: 7 }}>{editions.data?.length ?? 0}개 에디션</Text>

      <View style={{ marginTop: 16, flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'space-between' }}>
        {editions.data?.map((ed) => (
          <EditionCard key={ed.editionId} ed={ed} onOpen={openEdition} />
        ))}
      </View>
    </ScrollView>
  );
}
