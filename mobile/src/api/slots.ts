import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from './client';
import { useSession } from '../store/session';

export interface Category { code: string; name: string }
export interface Slot { id: number; categoryCodes: string[]; categoryLine: string }

export function useCategories() {
  const language = useSession((s) => s.language);
  return useQuery({
    queryKey: ['categories', language],
    queryFn: async (): Promise<Category[]> => {
      const { data } = await api.get('/categories', { params: { lang: language } });
      return data;
    },
  });
}

export function useSlots() {
  const userId = useSession((s) => s.userId);
  return useQuery({
    queryKey: ['slots', userId],
    enabled: userId != null,
    queryFn: async (): Promise<Slot[]> => {
      const { data } = await api.get('/slots');
      return data;
    },
  });
}

export function useCreateSlot() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (categoryCodes: string[]): Promise<Slot> => {
      const { data } = await api.post('/slots', { categoryCodes });
      return data;
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['slots'] }),
  });
}

export function useUpdateSlot() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: { slotId: number; categoryCodes: string[] }): Promise<Slot> => {
      const { data } = await api.put(`/slots/${input.slotId}`, { categoryCodes: input.categoryCodes });
      return data;
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['slots'] }),
  });
}

export function useDeleteSlot() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (slotId: number): Promise<void> => {
      await api.delete(`/slots/${slotId}`);
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['slots'] }),
  });
}
