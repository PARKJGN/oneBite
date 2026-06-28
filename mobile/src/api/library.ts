import { useQuery } from '@tanstack/react-query';
import { api } from './client';
import { useSession } from '../store/session';

export interface LibrarySlot {
  comboKey: string;
  categoryLine: string;
  editionCount: number;
  latestDate: string | null;
  active: boolean;
}
export interface LibraryEdition {
  editionId: number;
  issueDate: string;
  oneLine: string;
  read: boolean;
}

export function useLibrarySlots() {
  const userId = useSession((s) => s.userId);
  return useQuery({
    queryKey: ['library', 'slots', userId],
    enabled: userId != null,
    queryFn: async (): Promise<LibrarySlot[]> => (await api.get('/library/slots')).data,
  });
}

export function useLibraryEditions(comboKey: string) {
  const userId = useSession((s) => s.userId);
  return useQuery({
    queryKey: ['library', 'editions', userId, comboKey],
    enabled: userId != null && !!comboKey,
    queryFn: async (): Promise<LibraryEdition[]> =>
      (await api.get('/library/editions', { params: { comboKey } })).data,
  });
}
