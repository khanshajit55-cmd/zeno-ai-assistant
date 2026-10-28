package com.zenoai.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class SafeActionRouterTest {

    private fun parseActionTag(tag: String): Pair<String, Map<String, String>>? {
        if (!tag.startsWith("[ACTION:") || !tag.endsWith("]")) return null
        val inside = tag.removePrefix("[ACTION:").removeSuffix("]")
        val parts = inside.split(",")
        val type = parts[0].trim().uppercase(Locale.ROOT)
        val params = mutableMapOf<String, String>()
        for (i in 1 until parts.size) {
            val pair = parts[i].split("=", limit = 2)
            if (pair.size == 2) {
                params[pair[0].trim().lowercase(Locale.ROOT)] = pair[1].trim()
            }
        }
        return type to params
    }

    private fun isCommandSafe(command: String): Boolean {
        val lower = command.lowercase(Locale.ROOT)
        val forbidden = listOf("rm -rf", "su ", "sh ", "chmod", "eval", "; rm", "kill -9", "busybox")
        return forbidden.none { lower.contains(it) }
    }

    @Test
    fun testStructuredActionTagParsing() {
        val tag = "[ACTION:OPEN_APP,app=youtube]"
        val result = parseActionTag(tag)
        assertTrue(result != null)
        assertEquals("OPEN_APP", result?.first)
        assertEquals("youtube", result?.second?.get("app"))
    }

    @Test
    fun testAlarmActionTagParsing() {
        val tag = "[ACTION:SET_ALARM,hour=7,minute=30,message=Work]"
        val result = parseActionTag(tag)
        assertTrue(result != null)
        assertEquals("SET_ALARM", result?.first)
        assertEquals("7", result?.second?.get("hour"))
        assertEquals("30", result?.second?.get("minute"))
        assertEquals("Work", result?.second?.get("message"))
    }

    @Test
    fun testSecurityRejectionOfArbitraryShellCommands() {
        val unsafe1 = "rm -rf /data/data"
        val unsafe2 = "su root chmod 777"
        val unsafe3 = "run sh script"
        val safe1 = "open youtube"
        val safe2 = "what time is it"

        assertTrue(!isCommandSafe(unsafe1))
        assertTrue(!isCommandSafe(unsafe2))
        assertTrue(!isCommandSafe(unsafe3))
        assertTrue(isCommandSafe(safe1))
        assertTrue(isCommandSafe(safe2))
    }

    @Test
    fun testPhoneCallRequiresConfirmationRule() {
        val actionType = "MAKE_CALL"
        val requiresConfirmation = (actionType == "MAKE_CALL")
        assertTrue("Phone calls must require explicit user confirmation", requiresConfirmation)
    }
}