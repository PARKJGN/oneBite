'use client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from './api';
import { useSession } from '@/store/session';

// ── 카테고리 / 슬롯 ──
export interface Category { code: string; name: string }
export interface Slot { id: number; categoryCodes: string[]; categoryLine: string }
export function useCategories() {
  const language = useSession((s) => s.language);
  return useQuery({ queryKey: ['categories', language], queryFn: async () => (await api.get('/categories', { params: { lang: language } })).data as Category[] });
}
export function useSlots() {
  const userId = useSession((s) => s.userId);
  return useQuery({ queryKey: ['slots', userId], enabled: userId != null, queryFn: async () => (await api.get('/slots')).data as Slot[] });
}
export function useCreateSlot() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (categoryCodes: string[]) => (await api.post('/slots', { categoryCodes })).data as Slot,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['slots'] }),
  });
}
export function useUpdateSlot() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (i: { slotId: number; categoryCodes: string[] }) =>
      (await api.put(`/slots/${i.slotId}`, { categoryCodes: i.categoryCodes })).data as Slot,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['slots'] }),
  });
}
export function useDeleteSlot() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (slotId: number) => { await api.delete(`/slots/${slotId}`); },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['slots'] }),
  });
}

// ── 프로필 / 계정 ──
export interface Profile { userId: number; username: string | null; nickname: string; outputLanguage: 'ko' | 'en'; pushPermission: string; recoveryEmail: string | null }
export function useMe() {
  const userId = useSession((s) => s.userId);
  return useQuery({ queryKey: ['me', userId], enabled: userId != null, queryFn: async () => (await api.get('/me')).data as Profile });
}
export function useUpdateLanguage() {
  return useMutation({ mutationFn: async (language: 'ko' | 'en') => { await api.patch('/me', { outputLanguage: language }); } });
}
export function useDeleteAccount() {
  return useMutation({ mutationFn: async () => { await api.delete('/account'); } });
}

// ── 인증 ──
export function useLogin() {
  return useMutation({
    mutationFn: async (i: { username: string; password: string }) =>
      (await api.post('/auth/login', i)).data as { token: string; refreshToken: string; userId: number },
  });
}
export function useSocialLogin() {
  return useMutation({
    mutationFn: async (i: { provider: string; accessToken: string }) =>
      (await api.post('/auth/social', i)).data as { token: string; refreshToken: string; userId: number; isNew: boolean },
  });
}
// 웹 소셜 — authorization code를 백엔드로 보내 교환·검증(카카오/네이버)
export function useSocialCodeLogin() {
  return useMutation({
    mutationFn: async (i: { provider: string; code: string; redirectUri: string; state?: string }) =>
      (await api.post('/auth/social/code', i)).data as { token: string; refreshToken: string; userId: number; isNew: boolean },
  });
}
export function useSignup() {
  return useMutation({
    mutationFn: async (i: { username: string; password: string; nickname: string; recoveryEmail?: string | null }) =>
      (await api.post('/auth/signup', i)).data as { token: string; refreshToken: string; userId: number; nickname: string },
  });
}
export function usePasswordResetRequest() {
  return useMutation({
    mutationFn: async (username: string) => { await api.post('/auth/password-reset/request', { username }); },
  });
}
export function usePasswordResetConfirm() {
  return useMutation({
    mutationFn: async (i: { resetToken: string; newPassword: string }) => { await api.post('/auth/password-reset/confirm', i); },
  });
}

// ── 오늘 ──
export interface TodaySlot { comboKey: string; categoryLine: string; editionId: number | null; oneLine: string | null; read: boolean }
export interface TodayResponse { issueDate: string | null; slots: TodaySlot[]; banner: string | null }
export function useToday() {
  const userId = useSession((s) => s.userId);
  return useQuery({ queryKey: ['today', userId], enabled: userId != null, queryFn: async () => (await api.get('/today')).data as TodayResponse });
}

// ── 어제 핵심 뉴스 ──
export interface Highlight { title: string; source: string; categoryCode: string; editionId: number; url: string }
export interface YesterdayHighlights { page: number; totalPages: number; totalItems: number; items: Highlight[] }
export function useYesterday(page: number) {
  const userId = useSession((s) => s.userId);
  return useQuery({ queryKey: ['yesterday', userId, page], enabled: userId != null, queryFn: async () => (await api.get('/home/yesterday', { params: { page, size: 5 } })).data as YesterdayHighlights });
}

// ── 라이브러리(히스토리) ──
export interface LibrarySlot { comboKey: string; categoryLine: string; editionCount: number; latestDate: string | null; active: boolean }
export interface LibraryEdition { editionId: number; issueDate: string; oneLine: string; read: boolean }
export function useLibrarySlots() {
  const userId = useSession((s) => s.userId);
  return useQuery({ queryKey: ['lib', 'slots', userId], enabled: userId != null, queryFn: async () => (await api.get('/library/slots')).data as LibrarySlot[] });
}
export function useLibraryEditions(comboKey: string) {
  const userId = useSession((s) => s.userId);
  return useQuery({ queryKey: ['lib', 'eds', userId, comboKey], enabled: userId != null && !!comboKey, queryFn: async () => (await api.get('/library/editions', { params: { comboKey } })).data as LibraryEdition[] });
}

// ── 에디션 상세 ──
export interface EditionItem { title: string; source: string; url: string; categoryCode: string }
export interface CrossInsight { headline: string; body: string; items: EditionItem[] }
export interface EditionDetail { id: number; issueDate: string; oneLine: string; marketSummary: string[]; crossInsights: CrossInsight[]; references: string[]; bookmarked: boolean }
export function useEdition(id: number) {
  return useQuery({ queryKey: ['edition', id], queryFn: async () => (await api.get(`/editions/${id}`)).data as EditionDetail });
}

// ── 책갈피(FR-011b) ──
export interface BookmarkItem { editionId: number; issueDate: string; oneLine: string; comboKey: string }
export function useBookmarks() {
  const userId = useSession((s) => s.userId);
  return useQuery({ queryKey: ['bookmarks', userId], enabled: userId != null, queryFn: async () => (await api.get('/bookmarks')).data as BookmarkItem[] });
}
export function useSetBookmark(editionId: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (on: boolean) => {
      if (on) await api.put(`/editions/${editionId}/bookmark`);
      else await api.delete(`/editions/${editionId}/bookmark`);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['edition', editionId] });
      qc.invalidateQueries({ queryKey: ['bookmarks'] });
    },
  });
}
