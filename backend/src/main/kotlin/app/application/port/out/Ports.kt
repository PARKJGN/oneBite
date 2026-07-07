package app.application.port.out

import app.domain.model.Category
import app.domain.model.Slot
import app.domain.model.User

/** 아웃바운드 포트 — 도메인/애플리케이션이 의존하는 인터페이스(어댑터가 구현). */

interface UserRepository {
    fun save(user: User): User
    fun findByUsername(username: String): User?
    fun existsByUsername(username: String): Boolean
    fun existsByNickname(nickname: String): Boolean
    fun findById(id: Long): User?
    fun findByProvider(provider: String, providerId: String): User?
    fun delete(userId: Long) // 탈퇴: 사용자 행 삭제(슬롯·읽음/책갈피·발송기록은 FK cascade, 공유 Edition은 보존)
}

interface SlotRepository {
    fun save(slot: Slot): Slot
    fun findById(slotId: Long): Slot?
    fun findActiveByUserId(userId: Long): List<Slot>   // 발송·내 슬롯 화면
    fun findAllByUserId(userId: Long): List<Slot>       // 라이브러리(삭제 포함)
    fun countActiveByUserId(userId: Long): Int          // 슬롯 한도(≤3)는 활성만 계산
    fun deactivate(slotId: Long, userId: Long): Boolean // 소프트 삭제
}

interface CategoryRepository {
    fun findAllActive(): List<Category>
}

/** 비밀번호 해싱(FR-001a) — 도메인이 Spring Security에 직접 의존하지 않도록 포트화. */
interface PasswordHasher {
    fun hash(raw: String): String
    fun matches(raw: String, hash: String): Boolean
}

/** 로그인 토큰 발급(MVP: 불투명 토큰). 세션/검증 고도화는 후속. */
fun interface TokenIssuer {
    fun issue(userId: Long): String
}
