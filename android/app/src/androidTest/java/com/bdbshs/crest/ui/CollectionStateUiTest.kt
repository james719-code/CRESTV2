package com.bdbshs.crest.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.bdbshs.crest.ui.screens.*
import com.bdbshs.crest.ui.theme.CRESTTheme
import com.bdbshs.crest.ui.viewmodels.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CollectionStateUiTest(private val dark: Boolean) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "dark={0}")
        fun themes() = listOf(arrayOf(false), arrayOf(true))
    }
    @get:Rule val compose = createComposeRule()

    @Test fun documentsErrorRemainsVisibleAndRetryRecoversContent() {
        compose.setContent {
            CRESTTheme(darkTheme = dark) {
                var failed by remember { mutableStateOf(true) }
                val docs = if (failed) emptyList() else listOf(DocumentItem("d", "Proposal template", "A school research template", createdAt = 1000))
                DocumentsContent(
                    DocumentsUiState(isLoading = false, error = if (failed) "Check your connection." else null, allDocuments = docs),
                    docs, {}, {}, { failed = false }, {}, {}
                )
            }
        }
        compose.onNodeWithText("Could not update this page").assertIsDisplayed()
        compose.onNodeWithText("No school documents added yet").assertDoesNotExist()
        compose.onNodeWithText("Try again").performClick()
        compose.onNodeWithText("Proposal template").assertIsDisplayed()
    }

    @Test fun pendingAccountsEmptyStateLeadsToApprovedAccounts() {
        val approved = AccountItem.Student("a", "Approved Student", 123456789012, "STEM", accepted = true)
        compose.setContent {
            CRESTTheme(darkTheme = dark) {
                var status by remember { mutableStateOf(AccountStatus.Pending) }
                AccountsContent(
                    AccountsUiState(isLoading = false, allAccounts = listOf(approved), selectedStatus = status),
                    if (status == AccountStatus.Pending) emptyList() else listOf(approved),
                    {}, {}, { status = it }, {}, {}
                )
            }
        }
        compose.onNodeWithText("No accounts waiting for approval").assertIsDisplayed()
        compose.onNodeWithText("View approved accounts").performClick()
        compose.onNodeWithText("Approved Student").assertIsDisplayed()
        compose.onNodeWithText("LRN: 123456789012").assertExists()
    }

    @Test fun groupCardShowsThePaperAndSubmissionState() {
        var opened = 0
        compose.setContent {
            CRESTTheme(darkTheme = dark) {
                GroupRecordCard(
                    GroupItem("g", "Science Group", "STEM", listOf("a", "b"), true, false, "Water quality study", "quantitative"),
                    { opened++ }
                )
            }
        }
        compose.onNodeWithText("Water quality study").assertIsDisplayed()
        compose.onNodeWithText("Pending review").assertIsDisplayed()
        compose.onNodeWithText("2 members").assertExists()
        compose.onNodeWithText("Review submission").performClick()
        assertEquals(1, opened)
    }

    @Test fun readerFailureKeepsThePaperInformationAndWorkingRetry() {
        var retried = 0
        compose.setContent {
            CRESTTheme(darkTheme = dark) {
                ResearchOverview(ResearchItem("p", "Research title", listOf("Andrea"), "STEM"),
                    false, { retried++ }, readError = "Connect to download this paper.")
            }
        }
        compose.onNodeWithText("Paper could not open").assertIsDisplayed()
        compose.onNodeWithText("Research title").assertIsDisplayed()
        compose.onNodeWithText("Try again").performClick()
        assertEquals(1, retried)
    }

    @Test fun missingGoogleAccountExplainsRecovery() {
        var settingsOpened = 0
        compose.setContent {
            CRESTTheme(darkTheme = dark) {
                LoginScreenContent(false, {}, "No Google account is available on this device.", { settingsOpened++ })
            }
        }
        compose.onNodeWithText("Open account settings").performScrollTo().performClick()
        assertEquals(1, settingsOpened)
    }

    @Test fun submissionExplainsRequiredInputAndDisablesIncompleteSubmit() {
        compose.setContent {
            CRESTTheme(darkTheme = dark) {
                GroupUploadContent(GroupUploadUiState(), {}, {}, {}, {}, {})
            }
        }
        compose.onNodeWithText("Submit for review").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Add a title, select a research type, and choose a PDF to continue.").assertExists()
    }

    @Test fun storageEmptyStateExplainsOfflineAvailability() {
        var refreshed = 0
        compose.setContent {
            CRESTTheme(darkTheme = dark) {
                StorageContent(StorageUiState(isLoading = false), { refreshed++ }, {}, {}, {})
            }
        }
        compose.onNodeWithText("No papers saved on this device").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Refresh saved files").performScrollTo().performClick()
        assertEquals(1, refreshed)
    }
}
