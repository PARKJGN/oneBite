import { useQuery } from '@tanstack/react-query';
import { api } from './client';
import { useSession } from '../store/session';

export interface TodaySlot {
  comboKey: string;
  categoryLine: string;
  editionId: number | null;
  oneLine: string | null;
  read: boolean;
}
export interface TodayResponse {
  issueDate: string | null;
  slots: TodaySlot[];
  banner: string | null; // "오늘 뉴스레터 준비 중입니다" 등
}

export function useToday() {
  const userId = useSession((s) => s.userId);
  return useQuery({
    queryKey: ['today', userId],
    enabled: userId != null,
    queryFn: async (): Promise<TodayResponse> => (await api.get('/today')).data,
  });
}
