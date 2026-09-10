package com.monstera.harbor.spoof

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.net.Uri
import android.net.wifi.WifiInfo
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import org.json.JSONObject
import java.util.Locale
import java.util.TimeZone

class WorkSpoofXposedModule : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == MODULE_PACKAGE || lpparam.packageName == "$MODULE_PACKAGE.debug" || lpparam.packageName == "android") return
        XposedBridge.hookAllMethods(Application::class.java, "attach", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val context = param.args.firstOrNull() as? Context ?: return
                val config = loadConfig(context) ?: return
                runCatching { installHooks(lpparam, config) }
                    .onFailure { XposedBridge.log("WorkSpoof: falha ao instalar hooks em ${lpparam.packageName}: ${it.javaClass.simpleName}") }
            }
        })
    }

    private data class Config(val profile: SpoofProfile, val enabled: Set<String>)

    private fun loadConfig(context: Context): Config? {
        val authorities = listOf("$MODULE_PACKAGE.spoof", "$MODULE_PACKAGE.debug.spoof")
        for (authority in authorities) {
            val bundle = runCatching {
                context.contentResolver.call(Uri.parse("content://$authority"), "profile", null, null)
            }.getOrNull() ?: continue
            val json = bundle.getString("json") ?: continue
            val enabled = bundle.getStringArrayList("enabledFields")?.toSet().orEmpty()
            return runCatching { Config(SpoofProfile.fromJson(json), enabled) }.getOrNull()
        }
        return null
    }

    private fun installHooks(lpparam: XC_LoadPackage.LoadPackageParam, config: Config) {
        val p = config.profile
        val e = config.enabled
        fun setString(owner: Class<*>, field: String, value: String, key: String) {
            if (key in e && value.isNotBlank()) runCatching { XposedHelpers.setStaticObjectField(owner, field, value) }
        }
        setString(Build::class.java, "BRAND", p.brand, SpoofFields.BRAND)
        setString(Build::class.java, "MANUFACTURER", p.manufacturer, SpoofFields.MANUFACTURER)
        setString(Build::class.java, "MODEL", p.model, SpoofFields.MODEL)
        setString(Build::class.java, "DEVICE", p.deviceCode, SpoofFields.DEVICE)
        setString(Build::class.java, "PRODUCT", p.productName, SpoofFields.PRODUCT)
        setString(Build::class.java, "BOARD", p.board, SpoofFields.BOARD)
        setString(Build::class.java, "HARDWARE", p.hardware, SpoofFields.HARDWARE)
        setString(Build::class.java, "ID", p.buildId, SpoofFields.BUILD_ID)
        setString(Build::class.java, "DISPLAY", p.buildDisplayId, SpoofFields.DISPLAY_ID)
        setString(Build::class.java, "FINGERPRINT", p.buildFingerprint, SpoofFields.FINGERPRINT)
        setString(Build::class.java, "SERIAL", p.serial, SpoofFields.SERIAL)
        setString(Build.VERSION::class.java, "RELEASE", p.buildRelease, SpoofFields.RELEASE)
        setString(Build.VERSION::class.java, "SECURITY_PATCH", p.securityPatch, SpoofFields.PATCH)
        setString(Build.VERSION::class.java, "INCREMENTAL", p.buildIncremental, SpoofFields.INCREMENTAL)
        if (SpoofFields.SDK in e) p.buildSdk.toIntOrNull()?.let { runCatching { XposedHelpers.setStaticIntField(Build.VERSION::class.java, "SDK_INT", it) } }

        if (SpoofFields.REGION in e) {
            if (p.timezone.isNotBlank()) runCatching { TimeZone.setDefault(TimeZone.getTimeZone(p.timezone)) }
            if (p.locale.isNotBlank()) runCatching { Locale.setDefault(Locale.forLanguageTag(p.locale)) }
        }
        if (SpoofFields.SERIAL in e && p.serial.isNotBlank()) hookReturn(Build::class.java, "getSerial", p.serial)
        hookSettings(p, e)
        hookTelephony(p, e)
        hookNetworkIdentifiers(p, e)
        hookDisplay(p, e)
        hookMediaDrm(lpparam.classLoader, p, e)
        hookGoogleIds(lpparam.classLoader, p, e)
        XposedBridge.log("WorkSpoof: perfil '${p.name}' ativo em ${lpparam.packageName}")
    }

    private fun hookSettings(p: SpoofProfile, enabled: Set<String>) {
        if (SpoofFields.DEVICE_ID !in enabled || p.deviceId.isBlank()) return
        XposedBridge.hookAllMethods(Settings.Secure::class.java, "getString", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (param.args.lastOrNull() == Settings.Secure.ANDROID_ID) param.result = p.deviceId
            }
        })
    }

    private fun hookTelephony(p: SpoofProfile, enabled: Set<String>) {
        val values = listOf(
            Triple("getImei", SpoofFields.IMEI, p.imei), Triple("getDeviceId", SpoofFields.IMEI, p.imei),
            Triple("getMeid", SpoofFields.MEID, p.meid), Triple("getSubscriberId", SpoofFields.IMSI, p.imsi),
            Triple("getSimSerialNumber", SpoofFields.ICCID, p.iccid), Triple("getLine1Number", SpoofFields.PHONE, p.phoneNumber),
            Triple("getNetworkOperatorName", SpoofFields.OPERATOR, p.operatorAlpha), Triple("getSimOperatorName", SpoofFields.OPERATOR, p.operatorAlpha),
            Triple("getNetworkOperator", SpoofFields.OPERATOR, p.operatorNumeric), Triple("getSimOperator", SpoofFields.OPERATOR, p.operatorNumeric),
            Triple("getSimCountryIso", SpoofFields.REGION, p.simCountryIso),
        )
        values.filter { (_, key, value) -> key in enabled && value.isNotBlank() }
            .forEach { (method, _, value) -> hookReturn(TelephonyManager::class.java, method, value) }
    }

    private fun hookDisplay(p: SpoofProfile, enabled: Set<String>) {
        if (SpoofFields.SCREEN !in enabled) return
        val width = p.screenWidth.toIntOrNull() ?: return
        val height = p.screenHeight.toIntOrNull() ?: return
        val configuredDpi = p.screenDensity.toIntOrNull() ?: return
        XposedBridge.hookAllMethods(android.content.res.Resources::class.java, "getDisplayMetrics", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                (param.result as? DisplayMetrics)?.apply {
                    widthPixels = width; heightPixels = height; densityDpi = configuredDpi; density = configuredDpi / 160f; scaledDensity = configuredDpi / 160f
                }
            }
        })
    }

    private fun hookNetworkIdentifiers(p: SpoofProfile, enabled: Set<String>) {
        if (SpoofFields.MAC !in enabled || p.macAddress.isBlank()) return
        hookReturn(WifiInfo::class.java, "getMacAddress", p.macAddress)
        hookReturn(BluetoothAdapter::class.java, "getAddress", p.macAddress)
    }

    private fun hookMediaDrm(loader: ClassLoader, p: SpoofProfile, enabled: Set<String>) {
        if (SpoofFields.DRM_ID !in enabled || p.mediaDrmId.isBlank()) return
        val clazz = XposedHelpers.findClassIfExists("android.media.MediaDrm", loader) ?: return
        XposedBridge.hookAllMethods(clazz, "getPropertyByteArray", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (param.args.firstOrNull() == "deviceUniqueId") param.result = p.mediaDrmId.hexBytes()
            }
        })
    }

    private fun hookGoogleIds(loader: ClassLoader, p: SpoofProfile, enabled: Set<String>) {
        if (SpoofFields.AD_ID in enabled && p.advertisingId.isNotBlank()) {
            XposedHelpers.findClassIfExists("com.google.android.gms.ads.identifier.AdvertisingIdClient\$Info", loader)
                ?.let { hookReturn(it, "getId", p.advertisingId) }
        }
    }

    private fun hookReturn(clazz: Class<*>, method: String, value: Any) {
        XposedBridge.hookAllMethods(clazz, method, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) { param.result = value }
        })
    }

    private fun String.hexBytes(): ByteArray = chunked(2).mapNotNull { it.toIntOrNull(16)?.toByte() }.toByteArray()

    private var XC_MethodHook.MethodHookParam.result: Any?
        get() = getResult()
        set(value) = setResult(value)

    companion object { private const val MODULE_PACKAGE = "com.workspof.app" }
}
