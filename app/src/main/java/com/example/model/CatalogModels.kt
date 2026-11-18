package com.example.model

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.roundToInt

@Serializable
data class CoverColors(
    val from: String,
    val to: String,
    val ink: String,
)

@Serializable
data class Book(
    val id: String,
    val title: String,
    val author: String,
    val language: String,
    val category: String,
    val rating: Double,
    val reviews: Int,
    val pageCount: Int,
    val year: Int,
    val publisher: String,
    val added: String,
    val tags: List<String>,
    val description: String,
    val cover: CoverColors,
    val content: List<String>,
)

data class Language(
    val code: String,
    val name: String,
    val nativeName: String,
    val flag: String,
)

data class Category(
    val slug: String,
    val name: String,
    val icon: String,
    val tintFromHex: Long,
    val tintToHex: Long,
)

enum class SearchSort(val key: String, val label: String) {
    POPULAR("popular", "Popular"),
    NEWEST("newest", "Newest"),
    AZ("az", "A–Z"),
    RATING("rating", "Highest Rated"),
}

val LANGUAGES: List<Language> = listOf(
    Language("en", "English", "English", "🇬🇧"),
    Language("ml", "Malayalam", "മലയാളം", "🇮🇳"),
    Language("kn", "Kannada", "ಕನ್ನಡ", "🇮🇳"),
    Language("hi", "Hindi", "हिन्दी", "🇮🇳"),
    Language("ta", "Tamil", "தமிழ்", "🇮🇳"),
    Language("te", "Telugu", "తెలుగు", "🇮🇳"),
    Language("ar", "Arabic", "العربية", "🕌"),
    Language("other", "Other Languages", "World", "🌍"),
)

val CATEGORIES: List<Category> = listOf(
    Category("fiction", "Fiction", "📖", 0xFFDDD6FE, 0xFFEDE9FE),
    Category("mystery", "Mystery", "🔎", 0xFFE2E8F0, 0xFFF1F5F9),
    Category("romance", "Romance", "❤️", 0xFFFECDD3, 0xFFFFE4E6),
    Category("adventure", "Adventure", "🚀", 0xFFBAE6FD, 0xFFE0F2FE),
    Category("psychology", "Psychology", "🧠", 0xFFF5D0FE, 0xFFFAE8FF),
    Category("science", "Science", "🔬", 0xFF99F6E4, 0xFFCCFBF1),
    Category("history", "History", "📜", 0xFFFDE68A, 0xFFFEF3C7),
    Category("biography", "Biography", "👤", 0xFFC7D2FE, 0xFFE0E7FF),
    Category("education", "Education", "🎓", 0xFFBFDBFE, 0xFFDBEAFE),
    Category("technology", "Technology", "💻", 0xFFA5F3FC, 0xFFCFFAFE),
    Category("cybersecurity", "Cybersecurity", "🔐", 0xFFA7F3D0, 0xFFD1FAE5),
    Category("religion", "Religion", "🕌", 0xFFD9F99D, 0xFFECFCCB),
    Category("children", "Children's Books", "🧒", 0xFFFED7AA, 0xFFFFEDD5),
    Category("self-development", "Self Development", "🌱", 0xFFBBF7D0, 0xFFDCFCE7),
    Category("business", "Business", "💰", 0xFFFEF08A, 0xFFFEF9C3),
    Category("literature", "Literature", "📚", 0xFFE9D5FF, 0xFFF3E8FF),
)

fun categoryBySlug(slug: String): Category? = CATEGORIES.find { it.slug == slug }

fun languageByCode(code: String): Language? = LANGUAGES.find { it.code == code }

fun readingMinutes(book: Book): Int = max(3, (book.pageCount / 2.2).roundToInt())
