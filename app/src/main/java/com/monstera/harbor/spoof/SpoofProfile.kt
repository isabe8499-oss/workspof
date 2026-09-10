package com.monstera.harbor.spoof

import android.content.Context
import org.json.JSONObject
import java.security.SecureRandom
import java.util.Locale
import java.util.UUID

data class SpoofProfile(
    val id: String = "custom",
    val name: String = "Perfil personalizado",
    val brand: String = "google",
    val manufacturer: String = "Google",
    val model: String = "Pixel 7 Pro",
    val deviceCode: String = "cheetah",
    val productName: String = "cheetah",
    val board: String = "cheetah",
    val hardware: String = "cheetah",
    val boardPlatform: String = "gs201",
    val buildRelease: String = "15",
    val buildSdk: String = "35",
    val securityPatch: String = "2025-03-05",
    val buildId: String = "BP1A.250305.019",
    val buildDisplayId: String = "BP1A.250305.019",
    val buildIncremental: String = "13003188",
    val buildFingerprint: String = "google/cheetah/cheetah:15/BP1A.250305.019/13003188:user/release-keys",
    val screenWidth: String = "1440",
    val screenHeight: String = "3120",
    val screenDensity: String = "512",
    val operatorAlpha: String = "T-Mobile",
    val operatorNumeric: String = "310260",
    val simCountryIso: String = "us",
    val timezone: String = "America/Los_Angeles",
    val locale: String = "en-US",
    val deviceId: String = "",
    val macAddress: String = "",
    val imei: String = "",
    val meid: String = "",
    val imsi: String = "",
    val iccid: String = "",
    val phoneNumber: String = "",
    val advertisingId: String = "",
    val gsfId: String = "",
    val mediaDrmId: String = "",
    val appSetId: String = "",
    val serial: String = "",
    val enabledFields: Set<String> = SpoofFields.ALL,
    val requiredFields: Set<String> = emptySet(),
) {
    fun validate(): List<String> = buildList {
        if (brand.isBlank() || manufacturer.isBlank() || model.isBlank()) add("Marca, fabricante e modelo são obrigatórios")
        if (buildSdk.toIntOrNull() !in 29..36) add("O nível SDK deve estar entre 29 e 36")
        if (screenWidth.toIntOrNull() == null || screenHeight.toIntOrNull() == null || screenDensity.toIntOrNull() == null) add("Tela deve conter apenas números")
        if (imei.isNotBlank() && (imei.length != 15 || !imei.all(Char::isDigit) || !hasValidLuhn(imei))) add("IMEI inválido (esperado: 15 dígitos com Luhn)")
        if (operatorNumeric.isNotBlank() && !operatorNumeric.matches(Regex("\\d{5,6}"))) add("Código da operadora deve ter 5 ou 6 dígitos")
        if (advertisingId.isNotBlank() && runCatching { UUID.fromString(advertisingId) }.isFailure) add("Advertising ID deve ser um UUID")
        if (macAddress.isNotBlank() && !macAddress.matches(Regex("(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}"))) add("MAC inválido (use 00:11:22:33:44:55)")
        if (deviceId.isNotBlank() && !deviceId.matches(Regex("(?i)[0-9a-f]{16}"))) add("Android ID deve conter 16 caracteres hexadecimais")
        if (mediaDrmId.isNotBlank() && !mediaDrmId.matches(Regex("(?i)([0-9a-f]{2}){1,256}"))) add("MediaDrm ID deve ser hexadecimal com pares completos")
        if (locale.isNotBlank() && Locale.forLanguageTag(locale).language.isBlank()) add("Localidade inválida")
        requiredFields.forEach { key ->
            val present = when (key) {
                SpoofFields.DEVICE_ID -> deviceId
                SpoofFields.MAC -> macAddress
                SpoofFields.IMEI -> imei
                SpoofFields.MEID -> meid
                SpoofFields.IMSI -> imsi
                SpoofFields.ICCID -> iccid
                SpoofFields.PHONE -> phoneNumber
                SpoofFields.AD_ID -> advertisingId
                SpoofFields.GSF_ID -> gsfId
                SpoofFields.DRM_ID -> mediaDrmId
                SpoofFields.APP_SET_ID -> appSetId
                SpoofFields.SERIAL -> serial
                else -> "ok"
            }
            if (present.isBlank()) add("${SpoofFields.label(key)} é obrigatório")
        }
    }

    fun randomizeAdvanced(): SpoofProfile = copy(
        deviceId = randomHex(16),
        macAddress = randomMac(),
        imei = randomImei(),
        meid = randomHex(14).uppercase(),
        imsi = "310260" + randomDigits(9),
        iccid = luhnComplete("8901260" + randomDigits(11)),
        phoneNumber = "+1" + randomDigits(10),
        advertisingId = UUID.randomUUID().toString(),
        gsfId = randomHex(16),
        mediaDrmId = randomHex(64),
        appSetId = UUID.randomUUID().toString(),
        serial = randomHex(12).uppercase(),
    )

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("brandLabel", manufacturer)
        put("modelLabel", model)
        put("summary", "$boardPlatform - Android $buildRelease")
        put("_workspofEnabledFields", org.json.JSONArray(enabledFields.sorted()))
        put("_workspofRequiredFields", org.json.JSONArray(requiredFields.sorted()))
        put("profile", JSONObject().apply {
            put("brand", brand); put("manufacturer", manufacturer); put("model", model)
            put("productName", productName); put("deviceCode", deviceCode); put("board", board)
            put("hardware", hardware); put("boardPlatform", boardPlatform)
            put("buildFingerprint", buildFingerprint); put("buildId", buildId)
            put("buildDisplayId", buildDisplayId); put("buildIncremental", buildIncremental)
            put("buildRelease", buildRelease); put("buildSdk", buildSdk.toIntOrNull() ?: 35)
            put("securityPatch", securityPatch); put("screenWidth", screenWidth.toIntOrNull() ?: 1080)
            put("screenHeight", screenHeight.toIntOrNull() ?: 2400); put("screenDensity", screenDensity.toIntOrNull() ?: 420)
            put("operatorAlpha", operatorAlpha); put("operatorNumeric", operatorNumeric)
            put("simOperatorAlpha", operatorAlpha); put("simOperatorNumeric", operatorNumeric)
            put("simCountryIso", simCountryIso); put("timezone", timezone); put("locale", locale)
            put("deviceId", deviceId); put("imei", imei); put("meid", meid); put("imsi", imsi)
            put("macAddress", macAddress)
            put("iccid", iccid); put("phoneNumber", phoneNumber); put("advertisingId", advertisingId)
            put("gsfId", gsfId); put("mediaDrmId", mediaDrmId); put("appSetId", appSetId); put("serial", serial)
        })
    }

    companion object {
        fun fromJson(text: String): SpoofProfile {
            val root = JSONObject(text)
            val p = root.optJSONObject("profile") ?: root
            fun value(key: String, fallback: String = "") = if (p.has(key) && !p.isNull(key)) p.get(key).toString() else fallback
            return SpoofProfile(
                id = root.optString("id", "imported"), name = root.optString("name", root.optString("modelLabel", value("model", "Importado"))),
                brand = value("brand"), manufacturer = value("manufacturer"), model = value("model"),
                deviceCode = value("deviceCode"), productName = value("productName"), board = value("board"),
                hardware = value("hardware"), boardPlatform = value("boardPlatform"), buildRelease = value("buildRelease"),
                buildSdk = value("buildSdk"), securityPatch = value("securityPatch"), buildId = value("buildId"),
                buildDisplayId = value("buildDisplayId"), buildIncremental = value("buildIncremental"),
                buildFingerprint = value("buildFingerprint"), screenWidth = value("screenWidth"),
                screenHeight = value("screenHeight"), screenDensity = value("screenDensity"), operatorAlpha = value("operatorAlpha"),
                operatorNumeric = value("operatorNumeric"), simCountryIso = value("simCountryIso"), timezone = value("timezone"),
                locale = value("locale", "en-US"), deviceId = value("deviceId"), imei = value("imei"), meid = value("meid"),
                macAddress = value("macAddress"),
                imsi = value("imsi"), iccid = value("iccid"), phoneNumber = value("phoneNumber"),
                advertisingId = value("advertisingId"), gsfId = value("gsfId"), mediaDrmId = value("mediaDrmId"),
                appSetId = value("appSetId"), serial = value("serial"),
                enabledFields = root.stringSet("_workspofEnabledFields", SpoofFields.ALL),
                requiredFields = root.stringSet("_workspofRequiredFields", emptySet()),
            )
        }

        private val random = SecureRandom()
        private fun randomDigits(length: Int) = buildString { repeat(length) { append(random.nextInt(10)) } }
        private fun randomHex(length: Int) = buildString { repeat(length) { append("0123456789abcdef"[random.nextInt(16)]) } }
        private fun randomMac(): String {
            val bytes = ByteArray(6).also(random::nextBytes)
            bytes[0] = ((bytes[0].toInt() and 0xfe) or 0x02).toByte()
            return bytes.joinToString(":") { "%02x".format(it.toInt() and 0xff) }
        }
        private fun randomImei() = luhnComplete("49015420" + randomDigits(6))
        private fun luhnComplete(prefix: String): String = prefix + luhnDigit(prefix)
        private fun luhnDigit(prefix: String): Int {
            val sum = prefix.reversed().mapIndexed { index, c ->
                var n = c.digitToInt()
                if (index % 2 == 0) { n *= 2; if (n > 9) n -= 9 }
                n
            }.sum()
            return (10 - sum % 10) % 10
        }
        private fun hasValidLuhn(value: String): Boolean = luhnComplete(value.dropLast(1)) == value
    }
}

object SpoofFields {
    const val BRAND = "brand"; const val MANUFACTURER = "manufacturer"; const val MODEL = "model"
    const val DEVICE = "device"; const val PRODUCT = "product"; const val BOARD = "board"; const val HARDWARE = "hardware"
    const val PLATFORM = "platform"; const val RELEASE = "release"; const val SDK = "sdk"; const val PATCH = "patch"
    const val BUILD_ID = "build_id"; const val DISPLAY_ID = "display_id"; const val INCREMENTAL = "incremental"
    const val FINGERPRINT = "fingerprint"; const val SCREEN = "screen"; const val OPERATOR = "operator"
    const val REGION = "region"; const val DEVICE_ID = "device_id"; const val IMEI = "imei"; const val MEID = "meid"
    const val IMSI = "imsi"; const val ICCID = "iccid"; const val PHONE = "phone"; const val AD_ID = "ad_id"
    const val GSF_ID = "gsf_id"; const val DRM_ID = "drm_id"; const val APP_SET_ID = "app_set_id"; const val SERIAL = "serial"
    const val MAC = "mac"
    val ALL = setOf(BRAND, MANUFACTURER, MODEL, DEVICE, PRODUCT, BOARD, HARDWARE, PLATFORM, RELEASE, SDK, PATCH,
        BUILD_ID, DISPLAY_ID, INCREMENTAL, FINGERPRINT, SCREEN, OPERATOR, REGION, DEVICE_ID, IMEI, MEID, IMSI,
        ICCID, PHONE, AD_ID, GSF_ID, DRM_ID, APP_SET_ID, SERIAL, MAC)
    val IDENTIFIER_FIELDS = setOf(DEVICE_ID, MAC, IMEI, MEID, IMSI, ICCID, PHONE, AD_ID, GSF_ID, DRM_ID, APP_SET_ID, SERIAL)
    fun label(key: String): String = when (key) {
        DEVICE_ID -> "Android ID"; MAC -> "MAC"; IMEI -> "IMEI"; MEID -> "MEID"; IMSI -> "IMSI"; ICCID -> "ICCID"
        PHONE -> "Número de telefone"; AD_ID -> "Google Advertising ID"; GSF_ID -> "GSF ID"; DRM_ID -> "MediaDrm ID"; APP_SET_ID -> "App Set ID"; SERIAL -> "Serial"
        else -> key
    }
}

private fun JSONObject.stringSet(key: String, fallback: Set<String>): Set<String> =
    optJSONArray(key)?.let { array -> buildSet { repeat(array.length()) { add(array.optString(it)) } } } ?: fallback

class SpoofRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun load(): SpoofProfile = prefs.getString(PROFILE, null)?.let { runCatching { SpoofProfile.fromJson(it) }.getOrNull() } ?: SpoofProfile()
    fun save(profile: SpoofProfile) { prefs.edit().putString(PROFILE, profile.toJson().toString()).apply() }
    fun isEnabled(): Boolean = prefs.getBoolean(ENABLED, false)
    fun setEnabled(enabled: Boolean) { prefs.edit().putBoolean(ENABLED, enabled).apply() }
    fun enabledPackages(): Set<String> = prefs.getStringSet(PACKAGES, emptySet())?.toSet().orEmpty()
    fun setPackageEnabled(packageName: String, enabled: Boolean) {
        val next = enabledPackages().toMutableSet().apply { if (enabled) add(packageName) else remove(packageName) }
        prefs.edit().putStringSet(PACKAGES, next).apply()
    }
    companion object { const val PREFS = "workspof_spoof"; const val PROFILE = "profile"; const val ENABLED = "enabled"; const val PACKAGES = "packages" }
}
