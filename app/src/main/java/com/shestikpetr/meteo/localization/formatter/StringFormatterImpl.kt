package com.shestikpetr.meteo.localization.formatter

import com.shestikpetr.meteo.localization.interfaces.StringFormatter
import java.text.MessageFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of StringFormatter using Java MessageFormat
 * Supports standard formatting patterns and pluralization
 */
@Singleton
class StringFormatterImpl @Inject constructor() : StringFormatter {

    override fun format(template: String, vararg args: Any): String {
        return try {
            if (args.isEmpty()) {
                template
            } else {
                MessageFormat.format(template, *args)
            }
        } catch (e: Exception) {
            // Fallback to simple string replacement for basic cases
            var result = template
            args.forEachIndexed { index, arg ->
                result = result.replace("{$index}", arg.toString())
            }
            result
        }
    }

    override fun formatPlural(template: String, count: Int, vararg args: Any): String {
        return try {
            // Support for basic pluralization
            // Template format: "singular|plural" or "{0,choice,0#no items|1#one item|1<{0} items}"
            val allArgs = arrayOf(count, *args)

            if (template.contains("|")) {
                // Simple plural format: "item|items"
                val parts = template.split("|")
                val pluralForm = if (count == 1) parts[0] else parts.getOrElse(1) { parts[0] }
                format(pluralForm, count, *args)
            } else {
                // MessageFormat choice format
                MessageFormat.format(template, *allArgs)
            }
        } catch (e: Exception) {
            // Fallback
            format(template, count, *args)
        }
    }
}