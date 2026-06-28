import { useQuery } from '@tanstack/react-query';
import { api } from './client';
import { useSession } from '../store/session';

export interface Highlight {
  title: string;
  source: string;
  categoryCode: string;
  editionId: number;
  url: string; // 원문 기사 링크
}
export interface YesterdayHighlights {
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
  items: Highlight[];
}

// 어제 핵심 뉴스 — 페이지당 5개(FR-021)
export function useYesterdayHighlights(page: number, size = 5) {
  const userId = useSession((s) => s.userId);
  return useQuery({
    queryKey: ['home', 'yesterday', userId, page, size],
    enabled: userId != null,
    queryFn: async (): Promise<YesterdayHighlights> =>
      (await api.get('/home/yesterday', { params: { page, size } })).data,
  });
}
