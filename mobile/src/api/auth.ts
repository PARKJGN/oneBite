import { useMutation } from '@tanstack/react-query';
import { api } from './client';

export interface SignupInput {
  username: string;
  password: string;
  nickname: string;
  recoveryEmail?: string | null;
}
export interface SignupResult { userId: number; nickname: string; token: string; refreshToken: string }

export interface LoginInput { username: string; password: string }
export interface LoginResult { token: string; refreshToken: string; userId: number }

export function useSignup() {
  return useMutation({
    mutationFn: async (input: SignupInput): Promise<SignupResult> => {
      const { data } = await api.post('/auth/signup', input);
      return data;
    },
  });
}

// 아이디 중복/형식 확인 (4~20자 + 미사용이면 available)
export function useCheckUsername() {
  return useMutation({
    mutationFn: async (username: string): Promise<boolean> =>
      (await api.get('/auth/check-username', { params: { username } })).data.available,
  });
}

// 닉네임 중복 확인 (미사용이면 available)
export function useCheckNickname() {
  return useMutation({
    mutationFn: async (nickname: string): Promise<boolean> =>
      (await api.get('/auth/check-nickname', { params: { nickname } })).data.available,
  });
}

export function useLogin() {
  return useMutation({
    mutationFn: async (input: LoginInput): Promise<LoginResult> => {
      const { data } = await api.post('/auth/login', input);
      return data;
    },
  });
}

export interface SocialLoginResult { token: string; refreshToken: string; userId: number; isNew: boolean }

export function useSocialLogin() {
  return useMutation({
    mutationFn: async (input: { provider: string; accessToken: string }): Promise<SocialLoginResult> => {
      const { data } = await api.post('/auth/social', input);
      return data;
    },
  });
}
