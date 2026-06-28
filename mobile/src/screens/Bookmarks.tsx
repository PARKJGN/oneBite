import React from 'react';
import { FlatList, Pressable, Text, View } from 'react-native';
import { useBookmarks } from '../api/editions';
import { colors, radius } from '../theme';

// 책갈피 목록(FR-011b) — 영구 보관. 헤더 메뉴에서 진입, 항목 탭 시 상세로.
export default function Bookmarks({ navigation }: any) {
  const bookmarks = useBookmarks();

  return (
    <View style={{ flex: 1, backgroundColor: colors.bg, padding: 22, paddingTop: 18 }}>
      <Text style={{ fontSize: 28, fontWeight: '700', color: colors.ink }}>책갈피</Text>
      <Text style={{ fontSize: 14, color: colors.inkSoft, marginTop: 6 }}>책갈피한 에디션은 영구 보관돼요.</Text>

      <FlatList
        style={{ marginTop: 16 }}
        data={bookmarks.data ?? []}
        keyExtractor={(b) => String(b.editionId)}
        ListEmptyComponent={
          <Text style={{ fontSize: 14, color: colors.inkFaint }}>아직 책갈피한 에디션이 없어요.</Text>
        }
        renderItem={({ item }) => (
          <Pressable
            onPress={() => navigation.navigate('EditionDetail', { id: item.editionId })}
            style={({ pressed }) => [
              { padding: 14, backgroundColor: colors.surface, borderRadius: radius.md, marginBottom: 10, borderWidth: 1, borderColor: colors.border },
              pressed && { transform: [{ scale: 0.99 }], backgroundColor: '#f0f4f0' },
            ]}>
            <Text style={{ fontSize: 11, fontWeight: '700', color: colors.green }}>
              {item.comboKey.split('+').join(' · ')}
            </Text>
            <Text style={{ fontSize: 15, fontWeight: '600', color: colors.ink, marginTop: 4 }} numberOfLines={2}>
              {item.oneLine}
            </Text>
            <Text style={{ fontSize: 12, color: colors.inkFaint, marginTop: 6 }}>{item.issueDate}</Text>
          </Pressable>
        )}
      />
    </View>
  );
}
