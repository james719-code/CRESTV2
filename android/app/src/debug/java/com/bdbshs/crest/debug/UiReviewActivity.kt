package com.bdbshs.crest.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.bdbshs.crest.navigation.AppDestination
import com.bdbshs.crest.data.ThemeMode
import com.bdbshs.crest.ui.components.*
import com.bdbshs.crest.ui.screens.*
import com.bdbshs.crest.ui.theme.CRESTTheme
import com.bdbshs.crest.ui.viewmodels.*
import kotlinx.coroutines.launch

/** Debug source set only. Renders production content without authentication or database access. */
class UiReviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val firstPage = intent.getStringExtra("page") ?: "research"
        val firstMode = intent.getStringExtra("mode") ?: "data"
        val dark = intent.getBooleanExtra("dark", false)
        val fontScale = intent.getFloatExtra("fontScale", 1f)
        val auditMode = intent.getBooleanExtra("audit", false)
        setContent {
            CRESTTheme(darkTheme = dark) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    ReviewApp(firstPage, firstMode, auditMode)
                }
            }
        }
    }
}

private val reviewPapers = listOf(
    ResearchItem("paper-1", "Student perspectives on sustainable waste management in senior high school",
        listOf("Andrea Santos", "Miguel Reyes", "Sofia Cruz"), "STEM", downloads = 24,
        type = ResearchType.QUALITATIVE, createdAt = 1791244800000),
    ResearchItem("paper-2", "Digital study habits and academic performance among Grade 12 students",
        listOf("Isabel Ramos", "Noah Garcia"), "HUMSS", downloads = 12,
        type = ResearchType.QUANTITATIVE, createdAt = 1791072000000),
    ResearchItem("paper-3", "Small business budgeting practices of local food vendors",
        listOf("Lia Mendoza", "Ethan Torres"), "ABM", downloads = 8,
        type = ResearchType.QUALITATIVE, createdAt = 1790899200000),
    ResearchItem("paper-4", "Water quality monitoring with low-cost sensor systems",
        listOf("Sam Cruz", "Mia Flores", "Leo Diaz"), "TVL", unfinished = true, downloads = 5,
        type = ResearchType.QUANTITATIVE, createdAt = 1790726400000)
)
private val reviewDocuments = listOf(
    DocumentItem("doc-1", "Research proposal template", "Use this template for the title page, background, research questions, and study scope.", "local-1", 1791244800000),
    DocumentItem("doc-2", "Research ethics and consent forms", "Participant information sheet and consent forms for studies involving interviews or surveys.", "local-2", 1791072000000),
    DocumentItem("doc-3", "Submission checklist", "Check formatting, references, contributor names, and PDF completeness before submission.", "local-3", 1790899200000)
)
private val reviewGroups = listOf(
    GroupItem("group-1", "Sustainable Futures", "STEM", listOf("a", "b", "c"), true, false,
        "Student perspectives on sustainable waste management in senior high school", "qualitative", "local-1"),
    GroupItem("group-2", "Digital Learners", "HUMSS", listOf("d", "e"), true, false,
        "Digital study habits and academic performance among Grade 12 students", "quantitative", "local-2"),
    GroupItem("group-3", "Community Enterprise", "ABM", listOf("f", "g"), true, true,
        "Small business budgeting practices of local food vendors", "qualitative", "local-3"),
    GroupItem("group-4", "Sensor Lab", "TVL", listOf("h", "i", "j"))
)
private val reviewAccounts: List<AccountItem> = listOf(
    AccountItem.Student("a", "Andrea Santos", 123456789012, "STEM", accepted = false),
    AccountItem.Student("b", "Miguel Reyes", 123456789013, "STEM", accepted = true),
    AccountItem.Teacher("c", "Ms. Reyes", "teacher@example.test", access = false, upload_count = 2),
    AccountItem.Teacher("d", "Mr. Santos", "faculty@example.test", access = true, upload_count = 5)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewApp(firstPage: String, firstMode: String, auditMode: Boolean) {
    var page by remember { mutableStateOf(firstPage) }
    var mode by remember { mutableStateOf(firstMode) }
    var role by remember { mutableStateOf(if (firstPage == "student") UserType.STUDENT else UserType.TEACHER) }
    var menu by remember { mutableStateOf(false) }
    var query by remember(page) { mutableStateOf("") }
    var pending by remember { mutableStateOf(false) }
    var accepted by remember { mutableStateOf(false) }
    var accountStatus by remember { mutableStateOf(AccountStatus.Pending) }
    var favoriteIds by remember { mutableStateOf(if (firstPage == "favorites" && firstMode == "data") setOf("paper-1") else emptySet<String>()) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var strandFilter by remember { mutableStateOf(emptySet<String>()) }
    var selectedPaper by remember { mutableStateOf(reviewPapers.first()) }
    var selectedGroup by remember { mutableStateOf(reviewGroups.first()) }
    var formTitle by remember(page, mode) { mutableStateOf(if (mode == "data") reviewPapers.first().title else "") }
    var formType by remember(page, mode) { mutableStateOf<ResearchType?>(if (mode == "data") ResearchType.QUALITATIVE else null) }
    var formStrand by remember(page, mode) { mutableStateOf<Strand?>(if (mode == "data") Strand("STEM") else null) }
    var formFile by remember(page, mode) { mutableStateOf<String?>(if (mode == "data") "research-paper.pdf" else null) }
    var formMembers by remember(page, mode) { mutableStateOf(if (mode == "data") reviewPapers.first().members else listOf("")) }
    var profileName by remember(page, mode) { mutableStateOf(if (mode == "data") "Andrea Santos" else "") }
    var profileLrn by remember(page, mode) { mutableStateOf(if (mode == "data") "123456789012" else "") }
    var profileEmail by remember(page, mode) { mutableStateOf(if (mode == "data") "faculty@example.test" else "") }
    var registration by remember(page, mode) { mutableStateOf(SignUpDetailsUiState(
        selectedRole = if (page == "signupStudent") UserType.STUDENT else UserType.TEACHER,
        selectedStrand = if (mode == "data") "STEM" else "", selectedGender = if (mode == "data") "Female" else "",
        isLoading = mode == "loading", error = if (mode == "error") "Registration could not be saved. Try again." else null
    )) }
    var storageSelected by remember { mutableStateOf(setOf<String>()) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun notice(message: String) { scope.launch { snackbar.showSnackbar("Local review: $message") } }
    val loading = mode == "loading"
    val error = if (mode == "error") "Could not connect. Check your connection and try again." else null
    val papers = if (mode == "empty" || loading || error != null) emptyList() else reviewPapers
    val documents = if (mode == "empty" || loading || error != null) emptyList() else reviewDocuments
    val groups = if (mode == "empty" || loading || error != null) emptyList() else reviewGroups
    val accounts = if (mode == "empty" || loading || error != null) emptyList() else reviewAccounts
    val route = when (page) {
        "research" -> AppDestination.Researches.route
        "favorites" -> AppDestination.Favorites.route
        "accounts" -> AppDestination.Accounts.route
        "about" -> AppDestination.AboutUs.route
        "documents" -> AppDestination.Documents.route
        "groups" -> AppDestination.Groups.route
        else -> AppDestination.Home.route
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (!auditMode || page !in listOf("login", "pending", "signup", "profile")) {
            Column {
                if (!auditMode) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("LOCAL DATA · UI REVIEW", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                            TextButton(onClick = { menu = true }, contentPadding = PaddingValues(0.dp), modifier = Modifier.heightIn(min = 32.dp)) { Text("Screens") }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                listOf("teacher", "student", "research", "favorites", "documents", "groups", "accounts", "paper", "group", "storage", "upload", "teacherUpload", "signup", "signupStudent", "signupTeacher", "login", "about", "pending").forEach { destination ->
                                    DropdownMenuItem(text = { Text(destination.replaceFirstChar { it.titlecase() }) }, onClick = {
                                        page = destination
                                        if (destination == "student") role = UserType.STUDENT
                                        if (destination == "teacher") role = UserType.TEACHER
                                        menu = false
                                    })
                                }
                            }
                        }
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("data", "empty", "loading", "error").forEach { state ->
                                FilterChip(mode == state, { mode = state }, label = { Text(state.replaceFirstChar { it.titlecase() }) })
                            }
                        }
                    }
                }
                }
                if (page !in listOf("login", "pending", "signup", "profile")) {
                    CrestTopAppBar(
                        title = when (page) {
                            "research" -> "Research"
                            "favorites" -> "Favorites"
                            "documents" -> "Documents"
                            "groups" -> "Groups"
                            "accounts" -> "Accounts"
                            "paper" -> "Research Details"
                            "group" -> "Group Details"
                            "storage" -> "Storage Management"
                            "upload" -> "Submit research"
                            "teacherUpload" -> "Add research paper"
                            "signupStudent", "signupTeacher" -> "Complete Your Profile"
                            "about" -> "About"
                            else -> "Home"
                        },
                        userRole = role,
                        onNavigationClick = if (page in listOf("paper", "group", "storage", "upload", "teacherUpload", "signupStudent", "signupTeacher")) ({ page = "teacher" }) else null
                    )
                }
            }
            }
        },
        bottomBar = {
            if (page in listOf("teacher", "student", "research", "favorites", "documents", "groups", "accounts", "about")) {
                CrestBottomNavBar(getBottomNavItems(role), route, {
                    page = when (it) {
                        AppDestination.Home -> if (role == UserType.STUDENT) "student" else "teacher"
                        AppDestination.Documents -> "documents"
                        AppDestination.Groups -> "groups"
                        else -> "research"
                    }
                })
            }
        }
    ) { padding ->
        val bodyModifier = Modifier.fillMaxSize().padding(padding)
        val retry = { mode = "data" }
        when (page) {
            "research", "favorites" -> ResearchLibraryContent(
                ResearchesUiState(isLoading = loading, searchQuery = query, error = error,
                    strands = listOf("STEM", "HUMSS", "ABM", "TVL", "GAS").map { Strand(it, it in strandFilter) },
                    favoriteResearchIds = favoriteIds, showFavoritesOnly = favoritesOnly || page == "favorites"),
                papers.filter { (strandFilter.isEmpty() || it.strand in strandFilter) && (query.isBlank() || it.title.contains(query, true) || it.members.any { name -> name.contains(query, true) }) &&
                    (!(favoritesOnly || page == "favorites") || it.id in favoriteIds) },
                page == "favorites", true,
                { query = it }, { notice("Filter and sort controls are available in the signed-in app") },
                { favoritesOnly = !favoritesOnly }, { query = ""; favoritesOnly = false }, retry,
                { selectedPaper = it; page = "paper" },
                { favoriteIds = if (it.id in favoriteIds) favoriteIds - it.id else favoriteIds + it.id },
                null, bodyModifier,
                onStrandToggle = { name, on -> strandFilter = if (on) strandFilter + name else strandFilter - name }
            )
            "documents" -> DocumentsContent(
                DocumentsUiState(isLoading = loading, error = error, searchQuery = query, allDocuments = documents, userRole = role),
                documents.filter { it.name.contains(query, true) || it.description.contains(query, true) },
                { query = it }, { notice("Sort documents") }, retry, { notice("Download ${it.name}") },
                { notice("Manage ${it.name}") }, bodyModifier, { page = if (role == UserType.STUDENT) "student" else "teacher" }
            )
            "groups" -> GroupsContent(
                GroupsUiState(isLoading = loading, error = error, searchQuery = query, allGroups = groups, showPendingOnly = pending, showAcceptedOnly = accepted),
                groups.filter { it.group_name.contains(query, true) && (!pending || (it.uploaded && !it.accepted_research)) && (!accepted || it.accepted_research) },
                { query = it }, { notice("Sort groups") }, { pending = it; accepted = false }, { accepted = it; pending = false },
                { pending = false; accepted = false; query = "" }, retry, { selectedGroup = it; page = "group" }, bodyModifier
            )
            "accounts" -> AccountsContent(
                AccountsUiState(isLoading = loading, error = error, searchQuery = query, allAccounts = accounts, selectedStatus = accountStatus),
                accounts.filter {
                    val approved = when (it) { is AccountItem.Student -> it.accepted; is AccountItem.Teacher -> it.access }
                    approved == (accountStatus == AccountStatus.Accepted) && it.name.contains(query, true)
                },
                { query = it }, { notice("Filter school accounts") }, { accountStatus = it }, retry,
                { notice("Review ${it.name}") }, bodyModifier
            )
            "student" -> StudentHomeContent(
                StudentHomeUiState(
                    isLoading = loading, loadError = error,
                    studentDetails = StudentDetails("Andrea Santos", "STEM", "GROUP-024"),
                    groupDetails = if (mode != "data") null else GroupDetails(
                        "Sustainable Futures", "STEM", "local-1", listOf("a", "b", "c"), true, "qualitative", false,
                        listOf("Please check the citations in Chapter 2 before your final submission."), reviewPapers.first().title
                    ),
                    memberNames = listOf("Andrea Santos", "Miguel Reyes", "Sofia Cruz"),
                    totalResearchCount = papers.size, strandResearchCount = papers.count { it.strand == "STEM" }, recentResearches = papers
                ),
                { notice("Leave group") }, { notice("Create group") }, { notice("Join group") },
                { page = "upload" }, { notice("Withdraw submission") }, { notice("Download certificate") },
                { selectedPaper = reviewPapers.first { paper -> paper.id == it }; page = "paper" }, bodyModifier, retry
            )
            "teacher" -> TeacherHomeContent(
                TeacherHomeUiState(
                    isLoading = loading, loadError = error, teacherName = "Ms. Reyes",
                    dashboardItems = if (mode != "data") emptyList() else listOf(
                        DashboardCardItem("Total Researches", papers.size.toString(), Icons.Default.Description),
                        DashboardCardItem("Total Accounts", accounts.size.toString(), Icons.Default.Description),
                        DashboardCardItem("Pending Researches", groups.count { it.uploaded && !it.accepted_research }.toString(), Icons.Default.Description),
                        DashboardCardItem("Pending Accounts", "2", Icons.Default.Description)
                    ),
                    recentResearches = papers.map { SimpleResearch(it.id, it.title, it.createdAt, it.strand, it.members, it.type, it.downloads, it.unfinished) }
                ),
                { page = "teacherUpload" }, { selectedPaper = reviewPapers.first { paper -> paper.id == it }; page = "paper" },
                { page = "accounts" }, { page = "groups"; pending = true }, { notice("Upload document") }, bodyModifier, retry
            )
            "paper" -> ResearchOverview(selectedPaper, loading, { notice("Read PDF") }, bodyModifier)
            "login" -> Box(bodyModifier) { LoginScreenContent(loading, { notice("Google sign-in requires an account") }) }
            "profile" -> Box(bodyModifier) {
                ProfileBottomSheet("Ms. Reyes", "teacher@example.test", UserType.TEACHER, userPhotoUrl = null,
                    isOnline = true, currentTheme = if (androidx.compose.material3.MaterialTheme.colorScheme.background.red < .2f) ThemeMode.DARK else ThemeMode.LIGHT,
                    onAccountsClick = { page = "accounts" }, onFavoritesClick = { page = "favorites" },
                    onAboutClick = { page = "about" }, onStorageClick = { page = "storage" },
                    onThemeChange = { notice("Theme selected") }, onSignOutClick = { page = "login" }, onDismiss = {})
            }
            "group" -> GroupDetailsContent(
                GroupDetailUiState(isLoading = loading, group = if (mode == "data") selectedGroup else null,
                    memberNames = listOf("Andrea Santos", "Miguel Reyes", "Sofia Cruz"), error = error),
                { selectedPaper = reviewPapers.firstOrNull { it.title == selectedGroup.research_title } ?: reviewPapers.first(); page = "paper" },
                { notice("Approve submission") }, { notice("Return submission") }, { notice("Feedback text is local") }, retry, bodyModifier
            )
            "storage" -> {
                val cached = if (mode == "data") reviewPapers.take(2).mapIndexed { index, paper ->
                    CachedResearchItem(paper.id, paper.title, paper.strand, (index + 1) * 1024L * 1024L,
                        paper.createdAt, paper.id in storageSelected)
                } else emptyList()
                StorageContent(
                    StorageUiState(isLoading = loading, error = error, pdfCacheSize = cached.sumOf { it.sizeBytes },
                        totalAppCacheSize = cached.sumOf { it.sizeBytes } + 256L * 1024, cachedResearches = cached),
                    retry, { notice("Clear saved PDF confirmation") }, { notice("Clear cache confirmation") },
                    { storageSelected = if (it in storageSelected) storageSelected - it else storageSelected + it }, bodyModifier
                )
            }
            "upload" -> GroupUploadContent(
                GroupUploadUiState(title = formTitle, researchType = formType, selectedFileName = formFile, isLoading = loading, error = error),
                { formTitle = it }, { formType = it }, { formFile = "selected-review-paper.pdf" }, { formFile = null },
                { notice("Submit PDF for review") }, bodyModifier
            )
            "teacherUpload" -> TeacherUploadContent(
                UploadUiState(title = formTitle, members = formMembers, selectedStrand = formStrand,
                    selectedType = formType, selectedFileName = formFile, isLoading = loading, error = error),
                { formTitle = it }, { formStrand = it }, { formType = it },
                { index, name -> formMembers = formMembers.mapIndexed { itemIndex, value -> if (index == itemIndex) name else value } },
                { formMembers = formMembers + "" }, { index -> formMembers = formMembers.filterIndexed { itemIndex, _ -> itemIndex != index } },
                { formFile = "selected-review-paper.pdf" }, { formFile = null }, { notice("Add research to library") }, bodyModifier
            )
            "signup" -> Box(bodyModifier) { RoleSelectionScreen({ page = "signupStudent" }, { page = "signupTeacher" }) }
            "signupStudent" -> Box(bodyModifier) {
                StudentSignUpForm(
                    registration, profileName, { profileName = it }, profileLrn, { profileLrn = it },
                    listOf("STEM", "HUMSS", "ABM", "TVL", "GAS"),
                    { registration = registration.copy(selectedStrand = it, isStrandDropdownExpanded = false) },
                    { registration = registration.copy(isStrandDropdownExpanded = it) },
                    listOf("Male", "Female", "Other"),
                    { registration = registration.copy(selectedGender = it, isGenderDropdownExpanded = false) },
                    { registration = registration.copy(isGenderDropdownExpanded = it) },
                    mode == "error", mode == "error", mode == "error", mode == "error", { notice("Register student") }
                )
            }
            "signupTeacher" -> Box(bodyModifier) {
                TeacherSignUpForm(registration, profileName, { profileName = it }, profileEmail, { profileEmail = it },
                    mode == "error", mode == "error", { notice("Register teacher") })
            }
            "about" -> AboutUsScreen(bodyModifier)
            "pending" -> Box(bodyModifier) { PendingApprovalScreen { page = "login" } }
        }
    }
}
