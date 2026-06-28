package app.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

/** 공용 빈. Clock은 시간 의존 로직(예: 오늘 판정)을 테스트에서 고정 가능하게 주입한다. */
@Configuration
class AppConfig {
    @Bean
    fun clock(): Clock = Clock.systemDefaultZone()
}
