package com.aistudio.futureagent.agxjyz.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AgentSkill(
    val id: String,
    val name: String,
    val description: String,
    val markdownContent: String,
    val isEnabled: Boolean = true
)

object SkillManager {
    private val _skills = MutableStateFlow(
        listOf(
            AgentSkill(
                id = "skill_email",
                name = "Email Reader & Dispatcher",
                description = "Enables agent to fetch recent emails, summarize threads, and compose replies.",
                markdownContent = "# Email Skill (SKILL.md)\n\n## Capabilities\n- Read inbox messages\n- Parse sender & subject headers\n- Summarize content\n- Send replies via SMS or email API",
                isEnabled = true
            ),
            AgentSkill(
                id = "skill_accessibility",
                name = "Android Accessibility & File Controller",
                description = "Operates device UI, launches apps, and manages local files via Android APIs.",
                markdownContent = "# Accessibility & File Controller (SKILL.md)\n\n## Capabilities\n- Create, read, edit & list local files\n- Launch installed apps & open URLs\n- Simulate accessibility touch & input actions",
                isEnabled = true
            ),
            AgentSkill(
                id = "skill_notifications",
                name = "Notification Manager",
                description = "Inspects and manages device notifications across installed applications.",
                markdownContent = "# Notification Manager (SKILL.md)\n\n## Capabilities\n- Read active notification stream\n- Filter high-priority alerts\n- Dismiss or action notifications",
                isEnabled = true
            ),
            AgentSkill(
                id = "skill_scheduler",
                name = "Background Task Scheduler",
                description = "Schedules recurring and one-shot background agent routines via WorkManager.",
                markdownContent = "# Scheduler (SKILL.md)\n\n## Capabilities\n- Timed triggers\n- Periodic background sync\n- Automated report generation",
                isEnabled = true
            )
        )
    )
    val skills: StateFlow<List<AgentSkill>> = _skills.asStateFlow()

    fun toggleSkill(id: String, enabled: Boolean) {
        _skills.update { list ->
            list.map { if (it.id == id) it.copy(isEnabled = enabled) else it }
        }
    }
}
