package com.example.data

import android.content.Context
import com.example.model.Book
import com.example.model.SearchSort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.Locale

class BookRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cachedBooks: List<Book>? = null

    suspend fun getAllBooks(): List<Book> {
        cachedBooks?.let { return it }
        return withContext(Dispatchers.IO) {
            cachedBooks?.let { return@withContext it }
            val raw = context.assets.open("books.json").bufferedReader().use { it.readText() }
            val parsed = json.decodeFromString<List<Book>>(raw)
            cachedBooks = parsed
            parsed
        }
    }

    fun getCachedBooksOrEmpty(): List<Book> = cachedBooks ?: emptyList()

    suspend fun bookById(id: String): Book? = getAllBooks().find { it.id == id }

    fun byTag(books: List<Book>, tag: String): List<Book> = books.filter { tag in it.tags }

    fun searchBooks(
        books: List<Book>,
        query: String,
        languages: Set<String> = emptySet(),
        categories: Set<String> = emptySet(),
        minRating: Double = 0.0,
        sort: SearchSort = SearchSort.POPULAR,
    ): List<Book> {
        val q = query.trim().lowercase(Locale.ROOT)
        val filtered = books.filter { b ->
            if (q.isNotEmpty()) {
                val hay = "${b.title} ${b.author} ${b.category} ${b.language} ${b.description}"
                    .lowercase(Locale.ROOT)
                if (!hay.contains(q)) return@filter false
            }
            if (languages.isNotEmpty() && b.language !in languages) return@filter false
            if (categories.isNotEmpty() && b.category !in categories) return@filter false
            if (minRating > 0.0 && b.rating < minRating) return@filter false
            true
        }
        return when (sort) {
            SearchSort.NEWEST -> filtered.sortedByDescending { it.added }
            SearchSort.AZ -> filtered.sortedBy { it.title.lowercase(Locale.ROOT) }
            SearchSort.RATING -> filtered.sortedByDescending { it.rating }
            SearchSort.POPULAR -> filtered.sortedByDescending { it.reviews }
        }
    }
}
