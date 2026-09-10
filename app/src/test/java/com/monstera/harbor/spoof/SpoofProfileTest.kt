package com.monstera.harbor.spoof

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpoofProfileTest {
    @Test
    fun `imports the supplied profile shape`() {
        val profile = SpoofProfile.fromJson(
            """{"id":"pixel_7_pro","modelLabel":"Pixel 7 Pro","profile":{"brand":"google","manufacturer":"Google","model":"Pixel 7 Pro","buildSdk":35,"screenWidth":1440,"screenHeight":3120,"screenDensity":512}}""",
        )
        assertEquals("Pixel 7 Pro", profile.model)
        assertEquals("35", profile.buildSdk)
        assertEquals("1440", profile.screenWidth)
    }

    @Test
    fun `required identifiers prevent incomplete profile`() {
        val errors = SpoofProfile(requiredFields = setOf(SpoofFields.IMEI, SpoofFields.MAC)).validate()
        assertTrue(errors.any { it.contains("IMEI") })
        assertTrue(errors.any { it.contains("MAC") })
    }

    @Test
    fun `batch generator creates valid required identifiers`() {
        val profile = SpoofProfile().randomizeAdvanced()
        assertTrue(profile.imei.matches(Regex("\\d{15}")))
        assertTrue(profile.macAddress.matches(Regex("(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}")))
        assertTrue(profile.deviceId.isNotBlank())
    }

    @Test
    fun `round trip preserves required fields`() {
        val input = SpoofProfile(requiredFields = setOf(SpoofFields.IMEI), macAddress = "02:11:22:33:44:55")
        val output = SpoofProfile.fromJson(input.toJson().toString())
        assertTrue(SpoofFields.IMEI in output.requiredFields)
        assertEquals(input.macAddress, output.macAddress)
    }
}
