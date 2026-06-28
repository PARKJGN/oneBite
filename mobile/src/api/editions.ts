import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from './client';

export interface EditionDetail {
  id: number;
  issueDate: string;
  oneLine: string;
  marketSummary: string[];
  crossInsight: string | null;
  items: { title: string; source: string; url: string; categoryCode: string }[];
  references: string[];
  bookmarked: boolean;
}

export function useEdition(id: number) {
  return useQuery({
    queryKey: ['edition', id],
    queryFn: async (): Promise<EditionDetail> => (await api.get(`/editions/${id}`)).data,
  });
}

// ── 책갈피(FR-011b) ──
export interface BookmarkItem { editionId: number; issueDate: string; oneLine: string; comboKey: string }

export function useBookmarks() {
  return useQuery({
    queryKey: ['bookmarks'],
    queryFn: async (): Promise<BookmarkItem[]> => (await api.get('/bookmarks')).data,
  });
}

export function useSetBookmark(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (on: boolean) => {
      if (on) await api.put(`/editions/${id}/bookmark`);
      else await api.delete(`/editions/${id}/bookmark`);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['edition', id] });
      qc.invalidateQueries({ queryKey: ['bookmarks'] });
    },
  });
}
