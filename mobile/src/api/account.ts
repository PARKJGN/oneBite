import { useMutation, useQuery } from '@tanstack/react-query';
import { api } from './client';
import { useSession } from '../store/session';

export interface Profile {
  userId: number;
  username: string | null;
  nickname: string;
  timezone: string;
  outputLanguage: 'ko' | 'en';
  pushPermission: string;
  recoveryEmail: string | null;
}

export function useMe() {
  const userId = useSession((s) => s.userId);
  return useQuery({
    queryKey: ['me', userId],
    enabled: userId != null,
    queryFn: async (): Promise<Profile> => (await api.get('/me')).data,
  });
}

export function useUpdateLanguage() {
  return useMutation({
    mutationFn: async (language: 'ko' | 'en'): Promise<void> => {
      await api.patch('/me', { outputLanguage: language });
    },
  });
}

export function useDeleteAccount() {
  return useMutation({
    mutationFn: async (): Promise<void> => { await api.delete('/account'); },
  });
}
