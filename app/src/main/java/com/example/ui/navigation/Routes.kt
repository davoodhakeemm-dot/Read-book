package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object WelcomeRoute

@Serializable
data object OnboardingLanguageRoute

@Serializable
data object OnboardingCategoriesRoute

@Serializable
data object HomeRoute

@Serializable
data object ExploreRoute

@Serializable
data object SearchRoute

@Serializable
data object CategoriesRoute

@Serializable
data class CategoryDetailRoute(val slug: String)

@Serializable
data object LanguagesRoute

@Serializable
data class LanguageDetailRoute(val code: String)

@Serializable
data object LibraryRoute

@Serializable
data object FavoritesRoute

@Serializable
data object HistoryRoute

@Serializable
data object ProfileRoute

@Serializable
data object SettingsRoute

@Serializable
data class BookDetailRoute(val bookId: String)

@Serializable
data class ReaderRoute(val bookId: String, val page: Int? = null)

@Serializable
data object AuthRoute

@Serializable
data object SecretHomeRoute

@Serializable
data object MessagesHubRoute

@Serializable
data object ChatRoute

@Serializable
data object ChatSettingsRoute
