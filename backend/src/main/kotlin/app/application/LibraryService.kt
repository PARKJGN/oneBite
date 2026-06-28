package app.application

import app.domain.UserNotFoundException
import app.domain.service.ComboKey
import app.domain.port.`in`.LibraryEditionView
import app.domain.port.`in`.LibrarySlotView
import app.domain.port.`in`.LibraryUseCase
import app.domain.port.out.CategoryRepository
import app.domain.port.out.EditionRepository
import app.domain.port.out.SlotRepository
import app.domain.port.out.UserEditionStateRepository
import app.domain.port.out.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

@Service
class LibraryService(
    private val users: UserRepository,
    private val slots: SlotRepository,
    private val categories: CategoryRepository,
    private val editions: EditionRepository,
    private val states: UserEditionStateRepository,
    private val clock: Clock,
) : LibraryUseCase {

    @Transactional(readOnly = true)
    override fun slots(userId: Long): List<LibrarySlotView> {
        val user = users.findById(userId) ?: throw UserNotFoundException(userId)
        val zone = runCatching { ZoneId.of(user.timezone) }.getOrDefault(ZoneId.of("Asia/Seoul"))
        val today = LocalDate.ofInstant(clock.instant(), zone)
        val nameByCode = categories.findAllActive().associate { it.code to it.nameKo }

        // 활성+삭제 슬롯을 조합 키로 묶고(중복 제거), 활성 여부는 OR.
        // 구독 시작일(조합별 가장 이른 슬롯 createdAt)도 모은다 — 그 '다음날'부터 받은 것만 히스토리에 노출.
        val activeByCombo = HashMap<String, Boolean>()
        val codesByCombo = HashMap<String, List<String>>()
        val startByCombo = HashMap<String, LocalDate>()
        val unknownStart = HashSet<String>() // createdAt이 없는 레거시 슬롯이 섞이면 필터하지 않음
        slots.findAllByUserId(userId).forEach { s ->
            val key = ComboKey.of(s.categoryCodes)
            activeByCombo[key] = (activeByCombo[key] ?: false) || s.active
            codesByCombo[key] = s.categoryCodes
            val created = s.createdAt?.let { LocalDate.ofInstant(it, zone) }
            if (created == null) unknownStart.add(key)
            else startByCombo[key] = startByCombo[key]?.let { minOf(it, created) } ?: created
        }

        // 받은 후보(구독 다음날~) 수집 → 오늘-안읽음 제외 위해 읽음 상태 일괄 조회
        val candidates = codesByCombo.keys.associateWith { combo ->
            editions.findByComboAndLanguage(combo, user.outputLanguage) // 최신순
                .filter { isReceived(combo, it.issueDate, startByCombo, unknownStart) }
        }
        val readIds = states.findReadEditionIds(userId, candidates.values.flatten().mapNotNull { it.id })

        return codesByCombo.keys.mapNotNull { combo ->
            val eds = candidates.getValue(combo).filter { isVisible(it.issueDate, it.id, today, readIds) }
            if (eds.isEmpty()) return@mapNotNull null // 노출할 에디션이 있는 슬롯만 라이브러리에 표시
            LibrarySlotView(
                comboKey = combo,
                categoryLine = codesByCombo[combo]!!.joinToString(" · ") { nameByCode[it] ?: it },
                editionCount = eds.size,
                latestDate = eds.first().issueDate,
                active = activeByCombo[combo] ?: false,
            )
        }.sortedByDescending { it.latestDate }
    }

    @Transactional(readOnly = true)
    override fun editions(userId: Long, comboKey: String): List<LibraryEditionView> {
        val user = users.findById(userId) ?: throw UserNotFoundException(userId)
        val zone = runCatching { ZoneId.of(user.timezone) }.getOrDefault(ZoneId.of("Asia/Seoul"))
        val today = LocalDate.ofInstant(clock.instant(), zone)

        // 이 조합의 구독 시작일(가장 이른 슬롯 createdAt) — 다음날부터 받은 것만
        val comboSlots = slots.findAllByUserId(userId).filter { ComboKey.of(it.categoryCodes) == comboKey }
        val startByCombo = HashMap<String, LocalDate>()
        val unknownStart = HashSet<String>()
        comboSlots.forEach { s ->
            val created = s.createdAt?.let { LocalDate.ofInstant(it, zone) }
            if (created == null) unknownStart.add(comboKey)
            else startByCombo[comboKey] = startByCombo[comboKey]?.let { minOf(it, created) } ?: created
        }

        val received = editions.findByComboAndLanguage(comboKey, user.outputLanguage)
            .filter { isReceived(comboKey, it.issueDate, startByCombo, unknownStart) }
        val readIds = states.findReadEditionIds(userId, received.mapNotNull { it.id })
        return received
            .filter { isVisible(it.issueDate, it.id, today, readIds) } // 안 읽은 오늘자 제외
            .map {
                LibraryEditionView(
                    editionId = it.id!!,
                    issueDate = it.issueDate,
                    oneLine = it.content.oneLine,
                    read = it.id in readIds,
                )
            }
    }

    /** 구독 시작일 '다음날'부터 발송된 에디션만 '받은 것'으로 인정(가입 전 발송분 제외). 시작일 불명이면 전부 인정. */
    private fun isReceived(
        combo: String,
        issueDate: LocalDate,
        startByCombo: Map<String, LocalDate>,
        unknownStart: Set<String>,
    ): Boolean {
        if (combo in unknownStart) return true
        val start = startByCombo[combo] ?: return true
        return issueDate.isAfter(start)
    }

    /** 히스토리 노출 여부: 과거 발송분은 읽음 무관 노출, 오늘(이후) 발송분은 '읽은 것만' 노출(안 읽은 오늘자는 오늘 화면 전용). */
    private fun isVisible(issueDate: LocalDate, editionId: Long?, today: LocalDate, readIds: Set<Long>): Boolean {
        if (issueDate.isBefore(today)) return true
        return editionId != null && editionId in readIds
    }
}
