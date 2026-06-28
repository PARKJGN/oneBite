package app.domain.model

data class Category(
    val code: String,
    val nameKo: String,
    val nameEn: String,
    val active: Boolean = true,
) {
    fun nameFor(language: Language): String = if (language == Language.EN) nameEn else nameKo
}
