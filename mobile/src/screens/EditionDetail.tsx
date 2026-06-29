import React from 'react';
import { ScrollView, Text, TouchableOpacity, View } from 'react-native';
import { useEdition, useSetBookmark } from '../api/editions';
import { BookmarkIcon } from '../components/icons';
import { openArticle } from '../lib/openArticle';
import { colors } from '../theme';

// 에디션 상세 — 한줄평(왜) + 시장요약(영향) + 참고뉴스(출처). (US2 상세와 공용)
export default function EditionDetail({ route, navigation }: any) {
  const { id } = route.params;
  const edition = useEdition(id);
  const setBookmark = useSetBookmark(id);
  const e = edition.data;

  return (
    <View style={{ flex: 1, backgroundColor: colors.bg }}>
      <ScrollView contentContainerStyle={{ padding: 22, paddingTop: 14 }}>
      <View style={{ flexDirection: 'row', justifyContent: 'flex-end', alignItems: 'center' }}>
        {e ? (
          <TouchableOpacity onPress={() => setBookmark.mutate(!e.bookmarked)} disabled={setBookmark.isPending}
            style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
            <BookmarkIcon color={e.bookmarked ? colors.clay : colors.green} filled={e.bookmarked} size={18} />
            <Text style={{ fontSize: 15, fontWeight: '600', color: e.bookmarked ? colors.clay : colors.green }}>
              {e.bookmarked ? '책갈피됨' : '책갈피'}
            </Text>
          </TouchableOpacity>
        ) : null}
      </View>
      {!e ? (
        <Text style={{ marginTop: 20, color: colors.inkSoft }}>불러오는 중…</Text>
      ) : (
        <>
          <Text style={{ fontSize: 13, color: colors.inkFaint, marginTop: 12 }}>{e.issueDate}</Text>
          <Text style={{ fontSize: 14, fontWeight: '700', letterSpacing: 1, color: '#7a8b80', marginTop: 14 }}>한줄평</Text>
          <Text style={{ fontSize: 21, lineHeight: 32, fontWeight: '500', color: colors.ink, marginTop: 10 }}>{e.oneLine}</Text>

          <Text style={{ fontSize: 14, fontWeight: '700', letterSpacing: 1, color: '#7a8b80', marginTop: 22 }}>시장 요약</Text>
          {e.marketSummary.map((p, i) => (
            <Text key={i} style={{ fontSize: 16, lineHeight: 27, color: '#26352c', marginTop: 12 }}>{p}</Text>
          ))}

          <Text style={{ fontSize: 14, fontWeight: '700', letterSpacing: 1, color: '#7a8b80', marginTop: 26 }}>복합 정보</Text>
          {e.crossInsights.map((ci, ci_i) => (
            <View key={ci_i} style={{ marginTop: 16, padding: 16, backgroundColor: colors.greenSoft, borderRadius: 14 }}>
              <Text style={{ fontSize: 17, fontWeight: '700', lineHeight: 25, color: colors.ink }}>{ci.headline}</Text>
              <Text style={{ fontSize: 15, lineHeight: 24, color: '#26352c', marginTop: 8 }}>{ci.body}</Text>

              <Text style={{ fontSize: 11, fontWeight: '700', letterSpacing: 1, color: colors.green, marginTop: 14 }}>관련 뉴스</Text>
              {ci.items.map((it, i) => (
                <TouchableOpacity key={i} onPress={() => openArticle(it.url)}
                  style={{ flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 11, borderBottomWidth: i === ci.items.length - 1 ? 0 : 1, borderBottomColor: '#dde7de' }}>
                  <View style={{ flex: 1 }}>
                    <Text style={{ fontSize: 15, fontWeight: '500', color: colors.ink }}>{it.title}</Text>
                    <Text style={{ fontSize: 12, color: colors.inkFaint, marginTop: 4 }}>{it.source}</Text>
                  </View>
                  <Text style={{ fontSize: 18, color: '#b6c6ba' }}>›</Text>
                </TouchableOpacity>
              ))}
            </View>
          ))}
        </>
      )}
      </ScrollView>
    </View>
  );
}
