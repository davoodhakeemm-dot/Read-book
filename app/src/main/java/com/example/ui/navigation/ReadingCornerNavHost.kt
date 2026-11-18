package com.example.ui.navigation

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.MainViewModel
import com.example.SocialViewModel
import com.example.SocialViewModelFactory
import com.example.data.CloudSocialRepository
import com.example.data.NfcLinkResolution
import com.example.model.Book
import com.example.model.ChatState
import com.example.model.LibraryState
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.components.AppShell
import com.example.ui.components.ShellTab
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BookDetailScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.CategoryDetailScreen
import com.example.ui.screens.CategoryOnboardingScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LanguageDetailScreen
import com.example.ui.screens.LanguageOnboardingScreen
import com.example.ui.screens.LanguagesScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SecretHomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WelcomeScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

@Composable
fun ReadingCornerNavHost(
    viewModel: MainViewModel,
    books: List<Book>,
    libraryState: LibraryState,
    chatState: ChatState,
    widthSizeClass: WindowWidthSizeClass,
    pendingNfcToken: String? = null,
    onConsumeNfcToken: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var firebaseUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            firebaseUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val socialRepository = remember(context) { CloudSocialRepository(context.applicationContext) }
    val socialViewModel: SocialViewModel? = firebaseUser?.let { fbUser ->
        key(fbUser.uid) {
            viewModel(
                key = fbUser.uid,
                factory = SocialViewModelFactory(context.applicationContext, socialRepository, fbUser.uid),
            )
        }
    }

    val activeAccountId = socialViewModel?.activeAccountId?.collectAsStateWithLifecycle()?.value
    val rememberedPhone = socialViewModel?.rememberedPhone?.collectAsStateWithLifecycle()?.value
    val nfcNotice = socialViewModel?.pendingNfcNotice?.collectAsStateWithLifecycle()?.value
    var unauthenticatedNfcHint by remember { mutableStateOf<String?>(null) }

    // Resolve incoming NFC card taps or personal deep links safely
    LaunchedEffect(pendingNfcToken, firebaseUser?.uid) {
        val token = pendingNfcToken ?: return@LaunchedEffect
        val fbUser = firebaseUser
        val svm = socialViewModel
        if (fbUser == null || svm == null) {
            unauthenticatedNfcHint = "Personal Reading Corner NFC card recognized. Sign in to continue securely."
            navController.navigate(AuthRoute) { launchSingleTop = true }
            return@LaunchedEffect
        }
        val resolution = svm.resolveNfcCardToken(token)
        onConsumeNfcToken()
        when (resolution) {
            is NfcLinkResolution.Valid -> {
                if (svm.activeAccountId.value == resolution.accountId) {
                    navController.navigate(SecretHomeRoute) {
                        popUpTo(AuthRoute) { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    navController.navigate(AuthRoute) { launchSingleTop = true }
                }
            }
            else -> {
                navController.navigate(AuthRoute) { launchSingleTop = true }
            }
        }
    }

    fun navigateTopLevel(route: Any) {
        navController.navigate(route) {
            popUpTo(HomeRoute) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun selectTab(tab: ShellTab) {
        when (tab) {
            ShellTab.HOME -> navigateTopLevel(HomeRoute)
            ShellTab.EXPLORE -> navigateTopLevel(ExploreRoute)
            ShellTab.CATEGORIES -> navigateTopLevel(CategoriesRoute)
            ShellTab.LANGUAGES -> navigateTopLevel(LanguagesRoute)
            ShellTab.LIBRARY -> navigateTopLevel(LibraryRoute)
            ShellTab.FAVORITES -> navigateTopLevel(FavoritesRoute)
            ShellTab.HISTORY -> navigateTopLevel(HistoryRoute)
            ShellTab.PROFILE -> navigateTopLevel(ProfileRoute)
            ShellTab.SETTINGS -> navigateTopLevel(SettingsRoute)
        }
    }

    fun openSecretArea() {
        val target: Any = if (firebaseUser != null && activeAccountId != null) SecretHomeRoute else AuthRoute
        navController.navigate(target) {
            launchSingleTop = true
        }
    }

    fun exitSecretToHome() {
        navController.navigate(HomeRoute) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    @Composable
    fun InShell(activeTab: ShellTab?, content: @Composable () -> Unit) {
        AppShell(
            activeTab = activeTab,
            widthSizeClass = widthSizeClass,
            onSelectTab = ::selectTab,
            onOpenSearch = { navController.navigate(SearchRoute) { launchSingleTop = true } },
            onOpenSettings = { navigateTopLevel(SettingsRoute) },
            onSecretUnlock = ::openSecretArea,
            content = content,
        )
    }

    NavHost(
        navController = navController,
        startDestination = WelcomeRoute,
    ) {
        composable<WelcomeRoute> {
            WelcomeScreen(
                onStartReading = { navController.navigate(OnboardingLanguageRoute) },
                onExploreBooks = {
                    navController.navigate(HomeRoute) {
                        popUpTo(WelcomeRoute) { inclusive = true }
                    }
                    navController.navigate(ExploreRoute) {
                        launchSingleTop = true
                    }
                },
                onContinueHome = {
                    navController.navigate(HomeRoute) {
                        popUpTo(WelcomeRoute) { inclusive = true }
                    }
                },
                onSecretUnlock = ::openSecretArea,
            )
        }

        composable<OnboardingLanguageRoute> {
            LanguageOnboardingScreen(
                selectedLanguages = libraryState.languages,
                widthSizeClass = widthSizeClass,
                onToggleLanguage = viewModel::toggleLanguage,
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate(OnboardingCategoriesRoute) },
            )
        }

        composable<OnboardingCategoriesRoute> {
            CategoryOnboardingScreen(
                selectedCategories = libraryState.categories,
                widthSizeClass = widthSizeClass,
                onToggleCategory = viewModel::toggleCategory,
                onBack = { navController.popBackStack() },
                onContinue = {
                    viewModel.completeOnboarding()
                    navController.navigate(HomeRoute) {
                        popUpTo(WelcomeRoute) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable<HomeRoute> {
            InShell(activeTab = ShellTab.HOME) {
                HomeScreen(
                    books = books,
                    libraryState = libraryState,
                    widthSizeClass = widthSizeClass,
                    onBookClick = { book -> navController.navigate(BookDetailRoute(book.id)) },
                    onOpenExplore = { navigateTopLevel(ExploreRoute) },
                    onOpenSearch = { navController.navigate(SearchRoute) { launchSingleTop = true } },
                    onOpenSettings = { navigateTopLevel(SettingsRoute) },
                    onOpenProfile = { navigateTopLevel(ProfileRoute) },
                    onOpenLibrary = { navigateTopLevel(LibraryRoute) },
                    onOpenCategories = { navigateTopLevel(CategoriesRoute) },
                    onOpenLanguages = { navigateTopLevel(LanguagesRoute) },
                    onCategoryClick = { slug -> navController.navigate(CategoryDetailRoute(slug)) },
                    onLanguageClick = { code -> navController.navigate(LanguageDetailRoute(code)) },
                )
            }
        }

        composable<ExploreRoute> {
            InShell(activeTab = ShellTab.EXPLORE) {
                ExploreScreen(
                    books = books,
                    widthSizeClass = widthSizeClass,
                    onBookClick = { book -> navController.navigate(BookDetailRoute(book.id)) },
                    onOpenSearch = { navController.navigate(SearchRoute) { launchSingleTop = true } },
                    onOpenCategories = { navigateTopLevel(CategoriesRoute) },
                )
            }
        }

        composable<SearchRoute> {
            InShell(activeTab = null) {
                SearchScreen(
                    books = books,
                    repository = viewModel.bookRepository,
                    widthSizeClass = widthSizeClass,
                    onBookClick = { book -> navController.navigate(BookDetailRoute(book.id)) },
                    onExploreClick = { navigateTopLevel(ExploreRoute) },
                    onSecretChatTrigger = ::openSecretArea,
                )
            }
        }

        composable<CategoriesRoute> {
            InShell(activeTab = ShellTab.CATEGORIES) {
                CategoriesScreen(
                    widthSizeClass = widthSizeClass,
                    onCategoryClick = { slug -> navController.navigate(CategoryDetailRoute(slug)) },
                )
            }
        }

        composable<CategoryDetailRoute> { entry ->
            val args = entry.toRoute<CategoryDetailRoute>()
            InShell(activeTab = ShellTab.CATEGORIES) {
                CategoryDetailScreen(
                    slug = args.slug,
                    books = books,
                    widthSizeClass = widthSizeClass,
                    onBack = { navController.popBackStack() },
                    onBookClick = { book -> navController.navigate(BookDetailRoute(book.id)) },
                    onReadNow = { book -> navController.navigate(ReaderRoute(book.id)) },
                    onCategoryClick = { slug -> navController.navigate(CategoryDetailRoute(slug)) },
                    onExploreClick = { navigateTopLevel(ExploreRoute) },
                )
            }
        }

        composable<LanguagesRoute> {
            InShell(activeTab = ShellTab.LANGUAGES) {
                LanguagesScreen(
                    widthSizeClass = widthSizeClass,
                    onLanguageClick = { code -> navController.navigate(LanguageDetailRoute(code)) },
                )
            }
        }

        composable<LanguageDetailRoute> { entry ->
            val args = entry.toRoute<LanguageDetailRoute>()
            InShell(activeTab = ShellTab.LANGUAGES) {
                LanguageDetailScreen(
                    code = args.code,
                    books = books,
                    widthSizeClass = widthSizeClass,
                    onBack = { navController.popBackStack() },
                    onBookClick = { book -> navController.navigate(BookDetailRoute(book.id)) },
                    onLanguageClick = { code -> navController.navigate(LanguageDetailRoute(code)) },
                )
            }
        }

        composable<LibraryRoute> {
            InShell(activeTab = ShellTab.LIBRARY) {
                LibraryScreen(
                    books = books,
                    libraryState = libraryState,
                    widthSizeClass = widthSizeClass,
                    onBookClick = { book -> navController.navigate(BookDetailRoute(book.id)) },
                    onResumeBook = { book, page -> navController.navigate(ReaderRoute(book.id, page)) },
                    onExploreClick = { navigateTopLevel(ExploreRoute) },
                )
            }
        }

        composable<FavoritesRoute> {
            InShell(activeTab = ShellTab.FAVORITES) {
                FavoritesScreen(
                    books = books,
                    libraryState = libraryState,
                    widthSizeClass = widthSizeClass,
                    onBookClick = { book -> navController.navigate(BookDetailRoute(book.id)) },
                    onExploreClick = { navigateTopLevel(ExploreRoute) },
                )
            }
        }

        composable<HistoryRoute> {
            InShell(activeTab = ShellTab.HISTORY) {
                HistoryScreen(
                    books = books,
                    libraryState = libraryState,
                    widthSizeClass = widthSizeClass,
                    onBookClick = { book -> navController.navigate(BookDetailRoute(book.id)) },
                    onExploreClick = { navigateTopLevel(ExploreRoute) },
                )
            }
        }

        composable<ProfileRoute> {
            InShell(activeTab = ShellTab.PROFILE) {
                ProfileScreen(
                    libraryState = libraryState,
                    widthSizeClass = widthSizeClass,
                    onOpenSettings = { navigateTopLevel(SettingsRoute) },
                )
            }
        }

        composable<SettingsRoute> {
            InShell(activeTab = ShellTab.SETTINGS) {
                SettingsScreen(
                    libraryState = libraryState,
                    widthSizeClass = widthSizeClass,
                    onSetAppTheme = viewModel::setAppTheme,
                    onSetReaderTheme = viewModel::setReaderTheme,
                    onSetReaderFont = viewModel::setReaderFont,
                    onSetReaderLineSpacing = viewModel::setReaderLineSpacing,
                    onSetReminders = viewModel::setReminderEnabled,
                    onSetNewBooks = viewModel::setNewBooksEnabled,
                    onChangeLanguages = { navController.navigate(OnboardingLanguageRoute) },
                    onChangeCategories = { navController.navigate(OnboardingCategoriesRoute) },
                    onResetData = viewModel::resetLibrary,
                )
            }
        }

        composable<BookDetailRoute> { entry ->
            val args = entry.toRoute<BookDetailRoute>()
            val book = books.find { it.id == args.bookId }
            InShell(activeTab = null) {
                BookDetailScreen(
                    book = book,
                    allBooks = books,
                    libraryState = libraryState,
                    widthSizeClass = widthSizeClass,
                    onBack = { navController.popBackStack() },
                    onReadNow = { b -> navController.navigate(ReaderRoute(b.id)) },
                    onToggleFavorite = viewModel::toggleFavorite,
                    onToggleBookmark = viewModel::toggleBookmark,
                    onCategoryClick = { slug -> navController.navigate(CategoryDetailRoute(slug)) },
                    onLanguageClick = { code -> navController.navigate(LanguageDetailRoute(code)) },
                    onBookClick = { b -> navController.navigate(BookDetailRoute(b.id)) },
                    onExploreClick = { navigateTopLevel(ExploreRoute) },
                )
            }
        }

        composable<ReaderRoute> { entry ->
            val args = entry.toRoute<ReaderRoute>()
            val book = books.find { it.id == args.bookId }
            ReaderScreen(
                book = book,
                initialPage = args.page,
                libraryState = libraryState,
                onBack = { navController.popBackStack() },
                onExploreClick = {
                    navController.navigate(ExploreRoute) {
                        popUpTo(HomeRoute) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onFinishToLibrary = {
                    navController.navigate(LibraryRoute) {
                        popUpTo(HomeRoute) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onSaveProgress = viewModel::saveProgress,
                onMarkCompleted = viewModel::markCompleted,
                onToggleBookmark = { id, page -> viewModel.toggleBookmark(id, page) },
                onSetTheme = viewModel::setReaderTheme,
                onSetFont = viewModel::setReaderFont,
                onSetFontSize = viewModel::setReaderFontSize,
                onSetLineSpacing = viewModel::setReaderLineSpacing,
                onSetWidth = viewModel::setReaderWidth,
                onSetSpeechRate = viewModel::setSpeechRate,
            )
        }

        composable<AuthRoute> {
            AuthScreen(
                firebaseUser = firebaseUser,
                socialViewModel = socialViewModel,
                rememberedPhone = rememberedPhone,
                nfcNotice = nfcNotice ?: unauthenticatedNfcHint,
                onClearNfcNotice = {
                    unauthenticatedNfcHint = null
                    socialViewModel?.clearNfcNotice()
                },
                onSignedIn = {
                    navController.navigate(SecretHomeRoute) {
                        popUpTo(AuthRoute) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBackHome = ::exitSecretToHome,
            )
        }

        composable<SecretHomeRoute> {
            val svm = socialViewModel
            if (firebaseUser == null || svm == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(AuthRoute) {
                        popUpTo(SecretHomeRoute) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            } else {
                SecretHomeScreen(
                    socialViewModel = svm,
                    widthSizeClass = widthSizeClass,
                    onRequireAuth = {
                        navController.navigate(AuthRoute) {
                            popUpTo(SecretHomeRoute) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onBackHome = ::exitSecretToHome,
                    onLogOut = {
                        svm.signOutAccount()
                        GoogleAuthHelper.signOutAll(credentialManager, scope) {
                            navController.navigate(AuthRoute) {
                                popUpTo(SecretHomeRoute) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                )
            }
        }
    }
}
