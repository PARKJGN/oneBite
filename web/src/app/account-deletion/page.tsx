import type { Metadata } from 'next';
import { LegalPage } from '@/components/LegalPage';
import { CONTACT_EMAIL, OPERATOR, PRIVACY_URL } from '@/lib/legal';

export const metadata: Metadata = {
  title: '계정 삭제 안내 · oneBite',
  description: 'oneBite 계정과 데이터를 삭제하는 방법을 안내합니다.',
};

/**
 * Google Play 의 "계정 삭제 URL" 요건을 충족하는 페이지.
 * 앱을 설치하지 않은 사람도 삭제를 요청할 수 있는 경로(메일)를 반드시 함께 제공해야 한다.
 */
export default function AccountDeletionPage() {
  return (
    <LegalPage title="계정 삭제 안내">
      <p>
        <strong>앱 이름</strong> · oneBite
        <br />
        <strong>개발자</strong> · {OPERATOR}
      </p>
      <p>
        oneBite 계정과 계정에 연결된 데이터를 삭제하는 방법을 안내합니다. 삭제는 즉시 처리되며 되돌릴 수 없습니다.
      </p>

      <h2>방법 1 · 앱 또는 웹에서 직접 삭제</h2>
      <p>가장 빠른 방법이며 별도의 승인 절차 없이 바로 처리됩니다.</p>
      <ul>
        <li>oneBite 앱 또는 웹에 로그인합니다.</li>
        <li>메뉴에서 <strong>설정</strong>으로 이동합니다.</li>
        <li>화면 맨 아래 <strong>위험 구역</strong>의 <strong>회원 탈퇴</strong>를 누릅니다.</li>
        <li>안내 문구를 확인하고 탈퇴를 확정하면 계정과 데이터가 즉시 삭제됩니다.</li>
      </ul>

      <h2>방법 2 · 메일로 삭제 요청</h2>
      <p>
        앱을 이미 삭제했거나 로그인할 수 없는 경우, 아래 주소로 삭제를 요청할 수 있습니다.
      </p>
      <ul>
        <li>받는 사람 · <a href={`mailto:${CONTACT_EMAIL}?subject=oneBite%20계정%20삭제%20요청`}>{CONTACT_EMAIL}</a></li>
        <li>제목 · oneBite 계정 삭제 요청</li>
        <li>본문 · 가입에 사용한 아이디 또는 닉네임, 소셜 로그인을 사용했다면 그 제공자(Google · Naver · Kakao)</li>
      </ul>
      <p>
        본인 확인을 마친 뒤 <strong>영업일 기준 7일 이내</strong>에 삭제하고 회신드립니다. 본인 확인이 어려운 경우
        타인의 계정이 삭제되는 것을 막기 위해 요청이 거절될 수 있습니다.
      </p>

      <h2>삭제되는 데이터</h2>
      <p>탈퇴가 완료되면 아래 정보가 데이터베이스에서 <strong>영구히 삭제</strong>됩니다. 비활성 보관이나 유예 기간은 없습니다.</p>
      <ul>
        <li>계정 정보 · 아이디, 비밀번호 해시, 닉네임, 복구 이메일, 소셜 로그인 식별자</li>
        <li>구독 슬롯과 선택한 관심 카테고리</li>
        <li>에디션 읽음 기록과 책갈피</li>
        <li>뉴스레터 발송 이력</li>
        <li>푸시 알림 토큰 · 로그인 유지에 쓰인 리프레시 토큰</li>
      </ul>

      <h2>삭제되지 않는 데이터</h2>
      <ul>
        <li>
          모든 이용자에게 공통으로 발행된 <strong>뉴스레터 요약 본문</strong>. 특정 개인과 연결되지 않는 공용 콘텐츠이므로
          개인정보에 해당하지 않으며 서비스에 계속 남습니다.
        </li>
        <li>
          관계 법령이 보존을 요구하는 기록이 있는 경우, 해당 법령이 정한 기간 동안만 분리 보관한 뒤 파기합니다.
        </li>
      </ul>

      <h2>보관 기간</h2>
      <p>
        탈퇴 요청이 처리되는 즉시 삭제되며, 삭제된 데이터를 별도로 보관하는 기간은 <strong>없습니다.</strong>
        더 자세한 내용은 <a href={PRIVACY_URL}>개인정보처리방침</a>을 참고해 주세요.
      </p>
    </LegalPage>
  );
}
