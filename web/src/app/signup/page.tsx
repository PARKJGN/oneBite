'use client';
import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useSignup, useCheckUsername, useCheckNickname } from '@/lib/hooks';
import { useSession } from '@/store/session';
import { Button } from '@/components/ui/button';

const input = 'h-12 rounded-md border border-input bg-card px-4 text-ink outline-none focus:ring-2 focus:ring-ring';
const checkBtn = 'shrink-0 rounded-md bg-cloud px-4 text-sm font-semibold text-pine disabled:opacity-50';

const USERNAME_MIN = 4;
const USERNAME_MAX = 20;

type UsernameStatus = 'idle' | 'available' | 'taken' | 'invalid';
type NicknameStatus = 'idle' | 'available' | 'taken' | 'invalid';

export default function SignupPage() {
  const router = useRouter();
  const setSession = useSession((s) => s.setSession);
  const signup = useSignup();
  const checkUsername = useCheckUsername();
  const checkNickname = useCheckNickname();

  const [nickname, setNickname] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [recoveryEmail, setRecoveryEmail] = useState('');
  const [usernameStatus, setUsernameStatus] = useState<UsernameStatus>('idle');
  const [nicknameStatus, setNicknameStatus] = useState<NicknameStatus>('idle');
  const [error, setError] = useState<string | null>(null);

  const usernameValid = username.length >= USERNAME_MIN && username.length <= USERNAME_MAX;
  const nicknameValid = nickname.trim().length > 0;
  const passwordValid = password.length >= 8;
  const confirmMatch = confirm.length > 0 && confirm === password;

  const onCheckUsername = () => {
    if (!usernameValid) { setUsernameStatus('invalid'); return; }
    checkUsername.mutate(username, {
      onSuccess: (available) => setUsernameStatus(available ? 'available' : 'taken'),
      onError: () => setError('아이디 확인 중 오류가 발생했어요.'),
    });
  };

  const onCheckNickname = () => {
    if (!nicknameValid) { setNicknameStatus('invalid'); return; }
    checkNickname.mutate(nickname.trim(), {
      onSuccess: (available) => setNicknameStatus(available ? 'available' : 'taken'),
      onError: () => setError('닉네임 확인 중 오류가 발생했어요.'),
    });
  };

  const onSubmit = () => {
    setError(null);
    if (usernameStatus !== 'available') { setError('아이디 중복확인을 해주세요.'); return; }
    if (nicknameStatus !== 'available') { setError('닉네임 중복확인을 해주세요.'); return; }
    if (!passwordValid) { setError('비밀번호는 8자 이상이어야 해요.'); return; }
    if (!confirmMatch) { setError('비밀번호가 일치하지 않아요.'); return; }
    signup.mutate(
      { username, password, nickname: nickname.trim(), recoveryEmail: recoveryEmail.trim() || null },
      {
        onSuccess: (r) => { setSession(r.userId, r.token, r.refreshToken); router.push('/onboard'); },
        onError: (e: any) => {
          const code = e?.response?.data?.code as string | undefined;
          const message = e?.response?.data?.message as string | undefined;
          if (code === 'USERNAME_ALREADY_EXISTS') setUsernameStatus('taken');
          if (code === 'NICKNAME_ALREADY_EXISTS') setNicknameStatus('taken');
          setError(message ?? '입력값을 확인해 주세요.');
        },
      },
    );
  };

  return (
    <main className="mx-auto flex min-h-screen max-w-sm flex-col justify-center px-8">
      <h1 className="text-3xl font-bold text-ink">회원가입</h1>
      <p className="mt-2 text-ink-soft">이메일·휴대폰 인증 없이 바로 시작하세요.</p>

      <div className="mt-8 flex flex-col gap-3">
        {/* 닉네임 + 중복확인 */}
        <div>
          <div className="flex gap-2">
            <input
              value={nickname}
              onChange={(e) => { setNickname(e.target.value); setNicknameStatus('idle'); }}
              placeholder="닉네임"
              className={`${input} flex-1`}
            />
            <button type="button" onClick={onCheckNickname} disabled={checkNickname.isPending || !nickname} className={checkBtn}>
              {checkNickname.isPending ? '확인 중…' : '중복확인'}
            </button>
          </div>
          <p className={`mt-1.5 text-xs ${statusTone(nicknameStatus)}`}>{nicknameMsg(nicknameStatus)}</p>
        </div>

        {/* 아이디 + 중복확인 */}
        <div>
          <div className="flex gap-2">
            <input
              value={username}
              onChange={(e) => { setUsername(e.target.value); setUsernameStatus('idle'); }}
              placeholder={`아이디 (${USERNAME_MIN}~${USERNAME_MAX}자)`}
              autoCapitalize="none"
              className={`${input} flex-1`}
            />
            <button type="button" onClick={onCheckUsername} disabled={checkUsername.isPending || !username} className={checkBtn}>
              {checkUsername.isPending ? '확인 중…' : '중복확인'}
            </button>
          </div>
          <p className={`mt-1.5 text-xs ${statusTone(usernameStatus)}`}>{usernameMsg(usernameStatus)}</p>
        </div>

        <input value={password} onChange={(e) => setPassword(e.target.value)} type="password" placeholder="비밀번호 (8자 이상)" className={input} />
        <input value={confirm} onChange={(e) => setConfirm(e.target.value)} type="password" placeholder="비밀번호 확인" className={input} />
        {confirm.length > 0 && (
          <p className={`-mt-1 text-xs ${confirmMatch ? 'text-pine' : 'text-red-500'}`}>
            {confirmMatch ? '✓ 비밀번호가 일치해요' : '비밀번호가 일치하지 않아요'}
          </p>
        )}
        <input value={recoveryEmail} onChange={(e) => setRecoveryEmail(e.target.value)} type="email" placeholder="복구 이메일 (선택)" autoCapitalize="none" className={input} />
      </div>

      <div className="mt-3 rounded-md bg-cloud p-3.5">
        <p className="text-xs leading-relaxed text-ink-soft">
          복구 이메일을 입력하지 않으면 비밀번호를 잊었을 때 계정을 복구할 수 없어요.
        </p>
      </div>

      {error && <p className="mt-3 text-sm text-red-500">{error}</p>}

      <Button className="mt-4 w-full" onClick={onSubmit} disabled={signup.isPending}>
        {signup.isPending ? '가입 중…' : '가입하고 슬롯 만들기'}
      </Button>

      <p className="mt-5 text-center text-sm text-ink-faint">
        이미 계정이 있으신가요?{' '}
        <Link href="/login" className="font-semibold text-pine">로그인</Link>
      </p>
    </main>
  );
}

function usernameMsg(s: UsernameStatus): string {
  switch (s) {
    case 'available': return '✓ 사용 가능한 아이디예요';
    case 'taken': return '이미 사용 중인 아이디예요';
    case 'invalid': return `아이디는 ${USERNAME_MIN}~${USERNAME_MAX}자여야 해요`;
    default: return `${USERNAME_MIN}~${USERNAME_MAX}자, 가입 전 중복확인을 눌러주세요`;
  }
}
function nicknameMsg(s: NicknameStatus): string {
  switch (s) {
    case 'available': return '✓ 사용 가능한 닉네임이에요';
    case 'taken': return '이미 사용 중인 닉네임이에요';
    case 'invalid': return '닉네임을 입력해 주세요';
    default: return '가입 전 중복확인을 눌러주세요';
  }
}
function statusTone(s: UsernameStatus | NicknameStatus): string {
  if (s === 'available') return 'text-pine';
  if (s === 'taken' || s === 'invalid') return 'text-red-500';
  return 'text-ink-faint';
}
