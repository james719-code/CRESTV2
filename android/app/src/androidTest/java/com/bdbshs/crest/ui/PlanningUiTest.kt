package com.bdbshs.crest.ui

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bdbshs.crest.data.*
import com.bdbshs.crest.ui.screens.MilestonesContent
import com.bdbshs.crest.ui.screens.NotificationInboxContent
import com.bdbshs.crest.ui.theme.CRESTTheme
import com.bdbshs.crest.ui.viewmodels.PlanningUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Local fixtures only: no authentication or school data is used. */
@RunWith(AndroidJUnit4::class)
class PlanningUiTest {
    @get:Rule val compose = createComposeRule()
    private val group = PlanningGroup("g", "Science Group")
    private val milestone = ResearchMilestone("m", "g", "Research proposal", MilestoneStage.PROPOSAL, PlanningDates.today().minusDays(1).toString())

    @Test fun teacherCompletesAndReopensAMilestone() {
        compose.setContent {
            var item by remember { mutableStateOf(milestone) }
            CRESTTheme { MilestonesContent(PlanningUiState(PlanningRecords(groups = listOf(group), milestones = listOf(item), loading = false), teacher = true),
                onChange = { current, action, done -> item = current.copy(completedAt = if (action == "complete") 1 else 0); done() }) }
        }
        compose.onNodeWithContentDescription("Complete Research proposal").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Reopen Research proposal").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Complete Research proposal").assertExists()
    }
    @Test fun studentsSeeDeadlinesWithoutTeacherActions() {
        compose.setContent { CRESTTheme { MilestonesContent(PlanningUiState(PlanningRecords(groups = listOf(group), milestones = listOf(milestone), loading = false), teacher = false)) } }
        compose.onNodeWithText("Research proposal").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Add milestone").assertDoesNotExist()
        compose.onNodeWithContentDescription("Complete Research proposal").assertDoesNotExist()
        compose.onNodeWithContentDescription("Delete Research proposal").assertDoesNotExist()
    }
    @Test fun teacherCanCreateAValidatedPlan() {
        var saved: MilestoneInput? = null
        compose.setContent { CRESTTheme { MilestonesContent(PlanningUiState(PlanningRecords(groups = listOf(group), loading = false), teacher = true),
            onSave = { input, _, done -> saved = input; done() }) } }
        compose.onNodeWithText("Add milestone").performClick()
        compose.onNodeWithText("Choose research group").performClick()
        compose.onNodeWithText("Science Group").performClick()
        compose.onNode(hasSetTextAction() and hasText("Milestone title")).performTextInput("Chapter 2 draft")
        compose.onNodeWithText("Save milestone").performClick()
        compose.runOnIdle { assertEquals("Chapter 2 draft", saved?.title); assertEquals("g", saved?.groupId) }
    }
    @Test fun inboxMarksAllReadAndFiltersUnreadWithoutChangingOthers() {
        val updates = listOf(SchoolUpdate("one", "submission", "g", "A new submission", "Please review"), SchoolUpdate("two", "revision", "g", "Revision feedback", "Clarify sources"))
        compose.setContent {
            var notices by remember { mutableStateOf(updates) }
            CRESTTheme { NotificationInboxContent(PlanningUiState(PlanningRecords(groups = listOf(group), notifications = notices, loading = false), teacher = true),
                onRead = { ids, read, done -> notices = notices.map { if (it.id in ids) it.copy(read = read) else it }; done() }) }
        }
        compose.onNodeWithText("Mark all as read").performClick()
        compose.onNodeWithText("Unread only").performClick()
        compose.onNodeWithText("You’re all caught up").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("All notifications").performScrollTo().performClick()
        compose.onNodeWithText("A new submission").performScrollTo().assertIsDisplayed()
    }
}
