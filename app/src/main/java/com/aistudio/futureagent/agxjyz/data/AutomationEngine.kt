package com.aistudio.futureagent.agxjyz.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AutomationRule(
    val id: String,
    val name: String,
    val triggerType: String, // e.g. "BATTERY_LOW", "TIME_CRON", "INTERVAL"
    val conditionValue: String, // e.g. "20", "08:00", "30"
    val actionPrompt: String, // e.g. "Set volume to 10 and speak battery low"
    var isEnabled: Boolean = true
)

object AutomationEngine {
    private const val PREF_NAME = "sanna_automation_prefs"
    private const val KEY_RULES = "automation_rules_json"

    fun getRules(context: Context): List<AutomationRule> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_RULES, null) ?: return defaultRules()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<AutomationRule>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    AutomationRule(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        triggerType = obj.optString("triggerType"),
                        conditionValue = obj.optString("conditionValue"),
                        actionPrompt = obj.optString("actionPrompt"),
                        isEnabled = obj.optBoolean("isEnabled", true)
                    )
                )
            }
            if (list.isEmpty()) defaultRules() else list
        } catch (e: Exception) {
            defaultRules()
        }
    }

    fun saveRules(context: Context, rules: List<AutomationRule>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        for (rule in rules) {
            val obj = JSONObject()
            obj.put("id", rule.id)
            obj.put("name", rule.name)
            obj.put("triggerType", rule.triggerType)
            obj.put("conditionValue", rule.conditionValue)
            obj.put("actionPrompt", rule.actionPrompt)
            obj.put("isEnabled", rule.isEnabled)
            array.put(obj)
        }
        prefs.edit().putString(KEY_RULES, array.toString()).apply()
    }

    fun addRule(context: Context, name: String, triggerType: String, conditionValue: String, actionPrompt: String): String {
        val current = getRules(context).toMutableList()
        val id = "RULE_${System.currentTimeMillis()}"
        val rule = AutomationRule(id, name, triggerType, conditionValue, actionPrompt, true)
        current.add(rule)
        saveRules(context, current)
        return "Automation Rule '$name' created successfully (ID: $id)."
    }

    fun deleteRule(context: Context, ruleId: String): String {
        val current = getRules(context).toMutableList()
        val removed = current.removeAll { it.id == ruleId || it.name.equals(ruleId, ignoreCase = true) }
        if (removed) {
            saveRules(context, current)
            return "Automation Rule '$ruleId' deleted."
        }
        return "Rule '$ruleId' not found."
    }

    fun toggleRule(context: Context, ruleId: String, enabled: Boolean): String {
        val current = getRules(context).toMutableList()
        val rule = current.find { it.id == ruleId }
        if (rule != null) {
            rule.isEnabled = enabled
            saveRules(context, current)
            return "Rule '${rule.name}' state set to $enabled."
        }
        return "Rule not found."
    }

    private fun defaultRules(): List<AutomationRule> {
        return listOf(
            AutomationRule(
                id = "RULE_BATTERY_GUARD",
                name = "Low Battery Power Saver",
                triggerType = "BATTERY_LOW",
                conditionValue = "20",
                actionPrompt = "Battery below 20%. Toggle Bluetooth OFF and dim volume to 15%.",
                isEnabled = true
            ),
            AutomationRule(
                id = "RULE_DAILY_BRIEF",
                name = "Morning Intelligence Briefing",
                triggerType = "TIME_CRON",
                conditionValue = "08:00",
                actionPrompt = "Good morning! Read today's calendar events and weather forecast.",
                isEnabled = true
            )
        )
    }
}
