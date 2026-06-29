package app.adapter

import app.adapter.`in`.web.AuthController
import app.adapter.`in`.web.DeviceController
import app.adapter.`in`.web.EditionController
import app.adapter.`in`.web.SlotController
import app.application.port.out.DeviceTokenRepository
import app.application.port.out.EditionRepository
import app.application.port.out.UserRepository
import app.domain.model.Edition
import app.domain.model.EditionContent
import app.domain.model.EditionItem
import app.domain.model.Language
import app.domain.service.ComboKey
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.ZoneOffset

/** 탈퇴(FR-018a): 개인정보·슬롯 삭제, 공유 Edition은 보존. DELETE /account 를 HTTP 로 검증. */
class AccountDeletionIT : IntegrationTest() {

    @Autowired lateinit var auth: AuthController            // arrange
    @Autowired lateinit var slot: SlotController            // arrange
    @Autowired lateinit var editionApi: EditionController   // arrange(읽음 상태 생성)
    @Autowired lateinit var device: DeviceController        // arrange
    @Autowired lateinit var users: UserRepository           // assert
    @Autowired lateinit var editions: EditionRepository     // assert/arrange
    @Autowired lateinit var deviceTokens: DeviceTokenRepository // assert

    @Test
    fun `탈퇴 시 개인정보·슬롯은 삭제되고 공유 Edition은 남는다`() {
        val user = auth.signup(AuthController.SignupRequest("dave", "password123", "데이브"))
        slot.create(user.userId, SlotController.CreateSlotRequest(listOf("politics", "economy")))
        val today = LocalDate.now(ZoneOffset.UTC)
        val combo = ComboKey.of(listOf("politics", "economy"))
        val ed = editions.save(
            Edition(null, combo, Language.KO, today,
                EditionContent("핵심", listOf("요약"), null, listOf(EditionItem("t", "s", "u", "politics")), listOf("s"))),
        )
        editionApi.edition(user.userId, ed.id!!) // 읽음 상태 생성
        device.register(user.userId, DeviceController.RegisterRequest("tok-dave", "android")) // 기기 토큰 등록

        // act: 탈퇴 엔드포인트(HTTP)
        mockMvc.perform(delete("/account").header("Authorization", bearer(user.userId)))
            .andExpect(status().isNoContent)

        assertNull(users.findById(user.userId))                 // 사용자 삭제
        assertTrue(slot.list(user.userId).isEmpty())            // 슬롯 삭제(cascade)
        assertNotNull(editions.findById(ed.id!!))               // 공유 Edition 보존
        assertTrue(deviceTokens.findActiveTokens(user.userId).isEmpty()) // 기기 토큰 삭제(원칙 VI)
    }
}
