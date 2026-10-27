package com.bdbshs.crest.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.bdbshs.crest.data.ThemeMode
import com.bdbshs.crest.ui.components.ProfileBottomSheet
import com.bdbshs.crest.ui.screens.StorageContent
import com.bdbshs.crest.ui.screens.TeacherUploadContent
import com.bdbshs.crest.ui.screens.AccountActionDialog
import com.bdbshs.crest.ui.theme.CRESTTheme
import com.bdbshs.crest.ui.viewmodels.*
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ThemeInteractionTest(private val dark: Boolean) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "dark={0}")
        fun themes() = listOf(arrayOf(false), arrayOf(true))
    }
    @get:Rule val compose = createComposeRule()

    @Test fun savedPaperUsesReadableWidthAndSelectionWorks() {
        val title = "Student perspectives on sustainable waste management in senior high school"
        var density = 1f
        compose.setContent {
            CRESTTheme(darkTheme = dark) {
                density = LocalDensity.current.density
                var selected by remember { mutableStateOf(false) }
                Box(Modifier.width(360.dp).fillMaxSize()) {
                    StorageContent(
                        StorageUiState(isLoading = false, cachedResearches = listOf(
                            CachedResearchItem("paper", title, "STEM", 1048576, 1791244800000, selected)
                        )), {}, {}, {}, { selected = !selected }
                    )
                }
            }
        }
        val paper = compose.onNodeWithText(title, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        assertTrue("Saved paper title must use the card width", paper.fetchSemanticsNode().boundsInRoot.width >= 240f * density)
        val metadata = compose.onNodeWithText("STEM ·", substring = true, useUnmergedTree = true).assertIsDisplayed()
        assertTrue("The date must not wrap vertically", metadata.fetchSemanticsNode().boundsInRoot.height <= 40f * density)
        compose.onNodeWithText(title).performClick()
        compose.onNode(isToggleable()).assertIsOn()
    }

    @Test fun appearanceOptionsChangeThemeAndExposeSelection() {
        compose.setContent {
            var theme by remember { mutableStateOf(if (dark) ThemeMode.DARK else ThemeMode.LIGHT) }
            CRESTTheme(darkTheme = theme == ThemeMode.DARK) {
                ProfileBottomSheet("Ms. Reyes", "teacher@example.test", UserType.TEACHER, null, true,
                    currentTheme = theme, onAboutClick = {}, onThemeChange = { theme = it }, onSignOutClick = {}, onDismiss = {})
            }
        }
        compose.onNodeWithText("Dark").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("Light").performClick().assertIsSelected()
        compose.onNodeWithText("Dark").assertIsNotSelected()
        compose.onNodeWithText("System").performClick().assertIsSelected()
    }

    @Test fun removingPdfDisablesResearchSubmission() {
        compose.setContent {
            CRESTTheme(darkTheme = dark) {
                var file by remember { mutableStateOf<String?>("research.pdf") }
                TeacherUploadContent(
                    UploadUiState(title = "A complete research title", members = listOf("Andrea Santos"),
                        selectedStrand = Strand("STEM"), selectedType = ResearchType.QUALITATIVE, selectedFileName = file),
                    {}, {}, {}, { _, _ -> }, {}, {}, {}, { file = null }, {}
                )
            }
        }
        val submit = compose.onNode(hasText("Add research paper") and hasClickAction())
        submit.performScrollTo().assertIsEnabled()
        compose.onNodeWithContentDescription("Remove selected file").performScrollTo().performClick()
        submit.performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Select a PDF from the files on this device.").assertExists()
    }

    @Test fun accountReviewCanBeCancelledWithoutChangingAccess() {
        var cancelled = 0
        var approved = 0
        var denied = 0
        compose.setContent {
            CRESTTheme(darkTheme = dark) {
                AccountActionDialog(AccountItem.Student("pending", "Andrea Santos", 123456789012, "STEM", accepted = false),
                    false, { cancelled++ }, { approved++ }, { denied++ })
            }
        }
        compose.onNodeWithText("Approve").assertIsDisplayed()
        compose.onNodeWithText("Deny registration").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(1, cancelled)
        assertEquals(0, approved)
        assertEquals(0, denied)
    }

    @Test fun declinedStudentDeletionRequiresConfirmationAndSupportsCancel() {
        var deleted = 0
        var restored = 0
        compose.setContent {
            CRESTTheme(darkTheme = dark) {
                AccountActionDialog(
                    AccountItem.Student(uid = "declined", name = "Declined Student", declined = true),
                    false, {}, { restored++ }, {}, { deleted++ }
                )
            }
        }
        compose.onNodeWithText("Declined").assertIsDisplayed()
        compose.onNodeWithText("Restore access").assertIsDisplayed()
        compose.onNodeWithText("Delete student").performClick()
        compose.onNodeWithText("Delete declined student?").assertIsDisplayed()
        assertEquals(0, deleted)
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(0, deleted)
        compose.onNodeWithText("Delete student").performClick()
        compose.onNodeWithText("Confirm delete").performClick()
        assertEquals(1, deleted)
        assertEquals(0, restored)
    }
}
