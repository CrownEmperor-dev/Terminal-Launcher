package com.example.terminallauncher

import com.example.terminallauncher.data.AppModel
import com.example.terminallauncher.data.AsciiArtStyle
import com.example.terminallauncher.data.LauncherSettings
import com.example.terminallauncher.data.TerminalThemeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandEngineTest {

    @Test
    fun testMathExpressionEvaluation() {
        val expr1 = "12 + 8 * 2"
        val expr2 = "(100 - 20) / 4"
        val expr3 = "2 ^ 3"

        val val1 = evaluateMath(expr1)
        val val2 = evaluateMath(expr2)
        val val3 = evaluateMath(expr3)

        assertEquals(28.0, val1, 0.001)
        assertEquals(20.0, val2, 0.001)
        assertEquals(8.0, val3, 0.001)
    }

    @Test
    fun testAppModelAndSettingsDefaults() {
        val app = AppModel("YouTube", "com.google.android.youtube", "com.google.android.youtube.MainActivity")
        assertEquals("YouTube", app.label)
        assertEquals("com.google.android.youtube", app.packageName)

        val settings = LauncherSettings()
        assertEquals(TerminalThemeType.CRT_GREEN, settings.theme)
        assertTrue(settings.fastfetchOnStart)
        assertTrue(settings.autocompleteEnabled)
        assertEquals("YouTube", settings.aliases["yt"])
        assertEquals(15, settings.textSizeSp)
        assertEquals(AsciiArtStyle.DEFAULT, settings.asciiArtStyle)
    }

    private fun evaluateMath(expr: String): Double {
        val sanitized = expr.replace(" ", "")
        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < sanitized.length) sanitized[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                return parseExpression()
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    when {
                        eat('+'.code) -> x += parseTerm()
                        eat('-'.code) -> x -= parseTerm()
                        else -> return x
                    }
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    when {
                        eat('*'.code) -> x *= parseFactor()
                        eat('/'.code) -> x /= parseFactor()
                        else -> return x
                    }
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()

                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                    while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                    x = sanitized.substring(startPos, pos).toDouble()
                } else {
                    throw RuntimeException("Unexpected expression factor")
                }

                if (eat('^'.code)) x = Math.pow(x, parseFactor())

                return x
            }
        }.parse()
    }
}
