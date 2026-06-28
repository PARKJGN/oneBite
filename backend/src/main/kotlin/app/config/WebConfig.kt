package app.config

import app.adapter.`in`.web.CurrentUserId
import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/** @CurrentUserId → SecurityContext의 인증 principal(userId: Long) 주입. */
@Component
class CurrentUserIdResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(CurrentUserId::class.java) &&
            (parameter.parameterType == Long::class.java || parameter.parameterType == java.lang.Long::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any {
        val principal = SecurityContextHolder.getContext().authentication?.principal
        return principal as? Long ?: throw IllegalStateException("인증 정보가 없습니다")
    }
}

@Component
class WebConfig(private val currentUserId: CurrentUserIdResolver) : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(currentUserId)
    }
}
