import type { Metadata } from 'next';
import { LegalPage } from '@/components/LegalPage';
import { CONTACT_EMAIL, OPERATOR, PRIVACY_URL, TERMS_URL, ACCOUNT_DELETION_URL } from '@/lib/legal';

export const metadata: Metadata = {
  title: '지원 · oneBite',
  description: 'oneBite 사용 중 궁금한 점과 문제 해결 방법을 안내합니다.',
};

/**
 * App Store Connect 의 "지원 URL"(필수) 요건을 충족하는 페이지.
 * 심사자가 직접 열어보므로 연락 수단이 실제로 보여야 하고, 로그인 없이 접근 가능해야 한다
 * (AuthGuard 의 PUBLIC, AppChrome 의 NO_NAV 에 함께 등록).
 */
export default function SupportPage() {
  return (
    <LegalPage title="지원" showEffectiveDate={false}>
      <p>
        <strong>앱 이름</strong> · oneBite
        <br />
        <strong>개발자</strong> · {OPERATOR}
        <br />
        <strong>문의</strong> ·{' '}
        <a href={`mailto:${CONTACT_EMAIL}?subject=oneBite%20문의`}>{CONTACT_EMAIL}</a>
      </p>
      <p>
        사용 중 문제가 있거나 궁금한 점이 있으면 위 주소로 메일을 보내주세요. 확인하는 대로 답변드립니다.
        문제 상황을 알려주실 때 사용 기기와 앱 버전, 그리고 어떤 화면에서 무엇을 하다가 생긴 일인지 함께
        적어주시면 훨씬 빠르게 도와드릴 수 있습니다.
      </p>

      <h2>자주 묻는 질문</h2>

      <h3>뉴스레터는 언제 오나요?</h3>
      <p>
        매일 오전 8시에 하루 한 번 도착합니다. 알림도 이때 한 건만 발송되며, 하루 종일 속보 알림이 울리지
        않습니다.
      </p>

      <h3>가입했는데 &lsquo;오늘&rsquo; 화면이 비어 있어요.</h3>
      <p>
        먼저 슬롯을 만들어야 합니다. 관심 있는 카테고리를 골라 슬롯을 구성하면 그때부터 발송 대상이 됩니다.
        슬롯을 만든 뒤에도 화면이 비어 있다면, 아직 그날 발송분이 만들어지기 전일 수 있습니다. 이 경우
        &ldquo;오늘 뉴스레터 준비 중입니다&rdquo; 안내가 표시되며 다음 날 오전 8시부터는 정상적으로 도착합니다.
      </p>

      <h3>알림이 오지 않아요.</h3>
      <p>
        기기 설정에서 oneBite 의 알림이 허용되어 있는지 확인해 주세요. 앱의 <strong>설정</strong> 화면에서도
        현재 알림 상태를 확인할 수 있고, 거기서 기기 알림 설정으로 바로 이동할 수 있습니다. 알림을 꺼두어도
        앱을 열면 그날의 뉴스레터를 그대로 볼 수 있습니다.
      </p>

      <h3>슬롯은 몇 개까지 만들 수 있나요?</h3>
      <p>
        최대 3개이며, 슬롯 하나에 카테고리를 4개까지 담을 수 있습니다. 슬롯을 삭제하면 발송만 중단되고 지난
        에디션은 라이브러리에 그대로 남습니다.
      </p>

      <h3>지난 뉴스레터는 얼마나 볼 수 있나요?</h3>
      <p>
        라이브러리에서 최근 90일 치를 열람할 수 있습니다. 책갈피해 둔 에디션은 90일이 지나도 계속 보관됩니다.
      </p>

      <h3>비밀번호를 잊어버렸어요.</h3>
      <p>
        가입할 때 복구 이메일을 입력하셨다면 로그인 화면의 비밀번호 재설정을 이용해 주세요. 복구 이메일을
        등록하지 않으셨다면 계정을 복구할 수 없으며, 이 점은 가입 시에 안내됩니다. 소셜 로그인으로 가입한
        경우에는 해당 제공자(Google · Naver · Kakao)를 통해 로그인해 주세요.
      </p>

      <h3>계정을 삭제하고 싶어요.</h3>
      <p>
        앱의 <strong>설정 &gt; 위험 구역 &gt; 회원 탈퇴</strong>에서 직접 삭제할 수 있습니다. 앱을 이미
        삭제했다면 <a href={ACCOUNT_DELETION_URL}>계정 삭제 안내</a>를 참고해 메일로 요청해 주세요.
      </p>

      <h3>요약 내용이 사실과 다른 것 같아요.</h3>
      <p>
        요약문은 생성형 인공지능이 자동으로 작성하므로 원문의 맥락이 생략되거나 부정확하게 표현될 수 있습니다.
        각 에디션에는 근거가 된 기사의 출처가 함께 표시되니 원문을 확인해 주시고, 문제가 있는 요약을 발견하시면
        위 주소로 알려주시면 확인하겠습니다.
      </p>

      <h2>관련 문서</h2>
      <ul>
        <li>
          <a href={PRIVACY_URL}>개인정보처리방침</a>
        </li>
        <li>
          <a href={TERMS_URL}>이용약관</a>
        </li>
        <li>
          <a href={ACCOUNT_DELETION_URL}>계정 삭제 안내</a>
        </li>
      </ul>
    </LegalPage>
  );
}
