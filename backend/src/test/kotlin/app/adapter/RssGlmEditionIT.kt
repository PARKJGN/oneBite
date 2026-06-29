package app.adapter

import app.adapter.out.summarizer.LlmEditionSummarizer
import app.adapter.out.summarizer.OpenAiCompatChatClient
import app.application.port.out.FeedPort
import app.application.port.out.SummarizeInput
import app.domain.model.Language
import app.domain.service.ComboKey
import app.domain.service.Dedup
import app.domain.service.Ranking
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.time.Instant

/**
 * 라이브: 실제 RSS(politics+realestate) → Dedup/Ranking → **GLM 요약** → 정치+부동산 복합정보 1건.
 * 실 production 경로(RssFeedAdapter=실 HTTP + rss_sources 시드, OpenAiCompatChatClient=실 GLM)를 그대로 태운다.
 * GLM_API_KEY 가 있을 때만 실행(없으면 스킵 — CI 안전). 실행:
 *   GLM_API_KEY=... [GLM_MODEL=glm-5.2] ./gradlew test --tests "app.adapter.RssGlmEditionIT" -i
 * 결과(EditionContent + 바로 넣을 수 있는 INSERT SQL)를 콘솔에 출력한다.
 */
class RssGlmEditionIT : IntegrationTest() {

    @Autowired lateinit var feed: FeedPort // RssFeedAdapter — 실 HTTP + rss_sources(politics/realestate 시드)

    @Test
    fun `RSS 추출 → GLM 요약 → 정치+부동산 복합정보 생성`() {
        val glmKey = System.getenv("GLM_API_KEY").orEmpty()
        assumeTrue(glmKey.isNotBlank(), "GLM_API_KEY 미설정 — 라이브 생성 건너뜀")

        val categories = listOf("politics", "realestate")
        // 수동 데모: '지금 기준 직전 24h' 롤링 창 — 실행 시각의 최신 기사로 오늘자 정치+부동산을 본다.
        // (운영 8시 배치는 raw_articles 의 07:30 고정 창 사용. 이 러너는 라이브 피드 즉석 데모.)
        val until = Instant.now()
        val since = until.minus(Duration.ofHours(24))
        val articles = categories.flatMap { feed.fetch(it, since, until) }
        println("=== 수집 기사 ${articles.size}건 ===")
        articles.take(20).forEach { println("- [${it.categoryCode}] ${it.title} (${it.source})") }
        assumeTrue(articles.isNotEmpty(), "RSS 기사 0건(피드 가용성/창?) — 건너뜀")

        val top = Ranking.balancedTopClusters(Dedup.clusterByEvent(articles), categories).map { it.representative }
        println("=== 랭킹 상위 ${top.size}건 → GLM 요약 호출 ===")

        val base = System.getenv("GLM_BASE_URL")?.ifBlank { null } ?: "https://api.z.ai/api/paas/v4"
        val model = System.getenv("GLM_MODEL")?.ifBlank { null } ?: "glm-5.2"
        val client = OpenAiCompatChatClient(glmKey, base, model, 8192, objectMapper, disableThinking = true)
        val summarizer = LlmEditionSummarizer(client, objectMapper)

        val content = summarizer.summarize(SummarizeInput(categories, Language.KO, top))
        content.validate() // 품질 게이트(항목 1~5, 상투구 등)

        println("=== GLM 복합정보 결과 (provider: GLM $model) ===")
        println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(content))

        // 실 DB 에 넣을 수 있는 INSERT SQL
        val combo = ComboKey.of(categories) // politics+realestate
        fun q(s: String) = "'" + s.replace("'", "''") + "'"
        val crossSql = content.crossInsight?.let { q(it) } ?: "NULL"
        val sql = "INSERT INTO editions (combo_key, language, issue_date, one_line, market_summary, cross_insight, items, refs, status)\n" +
            "VALUES (${q(combo)}, 'ko', CURRENT_DATE, ${q(content.oneLine)}, " +
            "${q(objectMapper.writeValueAsString(content.marketSummary))}, $crossSql, " +
            "${q(objectMapper.writeValueAsString(content.items))}, " +
            "${q(objectMapper.writeValueAsString(content.references))}, 'ready')\n" +
            "ON CONFLICT (combo_key, language, issue_date) DO NOTHING;"
        println("=== 시드 SQL (실 DB 적용용) ===")
        println(sql)

        // 결과를 파일로도 남겨 회수 보장(gradle 로그 캡처와 무관). build/ 는 gitignore.
        val out = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(content) + "\n\n--- SQL ---\n" + sql
        java.io.File("build/rss-glm-edition.txt").writeText(out, Charsets.UTF_8)
        println("=== 결과 파일: backend/build/rss-glm-edition.txt ===")
    }
}
