package com.bdbshs.crest.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.bdbshs.crest.ui.screens.StudentHomeScreen
import com.bdbshs.crest.ui.theme.CRESTTheme
import com.bdbshs.crest.ui.viewmodels.StudentDetails
import com.bdbshs.crest.ui.viewmodels.StudentHomeUiState
import com.bdbshs.crest.ui.viewmodels.StudentHomeViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class StudentGroupDialogsTest(private val dark: Boolean) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "dark={0}")
        fun themes() = listOf(arrayOf(false), arrayOf(true))
    }
    @get:Rule val compose = createComposeRule()

    private fun show(): StudentHomeViewModel {
        val vm = mockk<StudentHomeViewModel>(relaxed = true)
        every { vm.uiState } returns MutableStateFlow(StudentHomeUiState(
            isLoading = false, studentDetails = StudentDetails("Andrea Santos", "STEM")
        ))
        compose.setContent {
            CRESTTheme(darkTheme = dark) {
                StudentHomeScreen(viewModel = vm, onNavigateToUpload = {}, onNavigateToResearchDetails = {})
            }
        }
        return vm
    }

    @Test fun creatingAGroupRequiresANameAndTrimsTheSubmittedValue() {
        val vm = show()
        compose.onNodeWithText("Create a group").performScrollTo().performClick()
        compose.onNodeWithText("Create").assertIsNotEnabled()
        compose.onNodeWithText("Group Name").performTextInput("  New Research Team  ")
        compose.onNodeWithText("Create").assertIsEnabled().performClick()
        verify(exactly = 1) { vm.createGroup("New Research Team") }
        compose.onNodeWithText("Create a New Group").assertDoesNotExist()
    }

    @Test fun joiningRejectsShortIdsAndCancelDoesNotJoin() {
        val vm = show()
        compose.onNodeWithText("Join a group").performScrollTo().performClick()
        compose.onNodeWithText("Join").assertIsNotEnabled()
        compose.onNodeWithText("Group ID").performTextInput("ABCDE")
        compose.onNodeWithText("Join").assertIsNotEnabled()
        compose.onNodeWithText("Group ID").performTextReplacement(" ABCDEF ")
        compose.onNodeWithText("Join").assertIsEnabled()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Join an Existing Group").assertDoesNotExist()
        verify(exactly = 0) { vm.joinGroup(any()) }
    }
}
