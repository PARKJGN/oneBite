package app.adapter.`in`.web

/** 인증된 사용자 ID 주입 표식 — JWT 필터가 SecurityContext에 넣은 userId를 컨트롤러 파라미터로 받는다. */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class CurrentUserId
