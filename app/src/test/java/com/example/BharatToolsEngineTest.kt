package com.example

import com.example.core.engine.QrToolkitEngine
import com.example.core.engine.TextToolkitEngine
import com.example.core.engine.UtilityCalculatorsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BharatToolsEngineTest {

    @Test
    fun textToolkit_countsAndFormattingWork() {
        val stats = TextToolkitEngine.analyze("Namaste Bharat.\nHow are you?")
        assertEquals(5, stats.words)
        assertEquals(2, stats.sentences)
        assertEquals("Namaste Bharat", TextToolkitEngine.toTitleCase("namaste bharat"))
        assertEquals("a\nb", TextToolkitEngine.removeDuplicateLines("a\nb\na"))
    }

    @Test
    fun qrToolkit_buildsUpiAndWifiPayloads() {
        val upi = QrToolkitEngine.buildUpiPayload("merchant@upi", "Bharat Store", "399", "Pro Pass")
        assertTrue(upi.startsWith("upi://pay?"))
        assertTrue(upi.contains("am=399.00"))
        assertTrue(upi.contains("cu=INR"))

        val wifi = QrToolkitEngine.buildWifiPayload("OfficeNet", "pass123", "WPA")
        assertEquals("WIFI:T:WPA;S:OfficeNet;P:pass123;;", wifi)
    }

    @Test
    fun calculators_gstEmiAndExpressionWork() {
        val gst = UtilityCalculatorsEngine.calculateGst(1000.0, 18.0, inclusive = false)
        assertEquals(180.0, gst.gstAmount, 0.01)
        assertEquals(1180.0, gst.totalAmount, 0.01)

        val emi = UtilityCalculatorsEngine.calculateEmi(100000.0, 12.0, 12).getOrThrow()
        assertEquals(8884.88, emi.monthlyEmi, 0.5)

        val expr = UtilityCalculatorsEngine.evaluateExpression("(12+8)*3/2").getOrThrow()
        assertEquals(30.0, expr, 0.001)
    }
}
