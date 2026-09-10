package com.monstera.harbor.spoof

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private data class TargetApp(val packageName: String, val label: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpoofProfileScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { SpoofRepository(context) }
    var profile by remember { mutableStateOf(repository.load()) }
    var active by remember { mutableStateOf(repository.isEnabled()) }
    var advancedVisible by remember { mutableStateOf(true) }
    var appSearch by remember { mutableStateOf("") }
    var enabledPackages by remember { mutableStateOf(repository.enabledPackages()) }
    var message by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val installedApps = remember {
        @Suppress("DEPRECATION")
        context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            .filter { it.packageName != context.packageName && it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .map { TargetApp(it.packageName, context.packageManager.getApplicationLabel(it).toString()) }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Não foi possível ler o arquivo")
                SpoofProfile.fromJson(text)
            }.onSuccess { imported -> profile = imported; message = "JSON importado: ${imported.model}" }
                .onFailure { message = "JSON inválido: ${it.message ?: "formato não reconhecido"}" }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(profile.toJson().toString(2)) }
                ?: error("Não foi possível criar o arquivo")
        }.onSuccess { message = "Perfil exportado" }.onFailure { message = "Falha ao exportar: ${it.message}" }
    }
    fun toggleField(key: String, enabled: Boolean) {
        profile = profile.copy(enabledFields = profile.enabledFields.toMutableSet().apply { if (enabled) add(key) else remove(key) })
    }
    fun toggleRequired(key: String, required: Boolean) {
        profile = profile.copy(requiredFields = profile.requiredFields.toMutableSet().apply { if (required) add(key) else remove(key) })
    }
    fun save() {
        val errors = profile.validate()
        if (errors.isNotEmpty()) {
            message = errors.joinToString("\n")
            scope.launch { snackbarHostState.showSnackbar("Não foi possível salvar: ${errors.first()}") }
            return
        }
        runCatching {
            repository.save(profile)
            repository.setEnabled(active)
        }.onSuccess {
            message = "Perfil salvo neste usuário Android"
            scope.launch { snackbarHostState.showSnackbar("Perfil WorkSpoof salvo neste usuário") }
        }.onFailure {
            message = "Falha ao salvar: ${it.message ?: "erro desconhecido"}"
            scope.launch { snackbarHostState.showSnackbar("Falha ao salvar o perfil") }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("WorkSpoof · Perfil do aparelho") }, navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { RootSettingsCard() }
            item {
                Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Spoof neste perfil", fontWeight = FontWeight.Bold)
                                Text("A configuração fica isolada neste usuário Android. A aplicação nos apps exige LSPosed ativo e escopo selecionado.", style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(checked = active, onCheckedChange = { active = it })
                        }
                        message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/json", "text/plain")) }, modifier = Modifier.weight(1f)) { Text("Importar JSON") }
                    OutlinedButton(onClick = { exportLauncher.launch("${profile.id.ifBlank { "workspof" }}.json") }, modifier = Modifier.weight(1f)) { Text("Exportar") }
                }
            }
            item { SectionTitle("Detalhes do perfil", "Predefinição importada ou edição personalizada") }
            item { ProfileField("Nome do perfil", profile.name, { profile = profile.copy(name = it) }) }
            item { SectionTitle("Identidade", "Valores Build expostos aos apps selecionados") }
            item { EnabledField("Marca", profile.brand, SpoofFields.BRAND, profile, ::toggleField) { profile = profile.copy(brand = it) } }
            item { EnabledField("Fabricante", profile.manufacturer, SpoofFields.MANUFACTURER, profile, ::toggleField) { profile = profile.copy(manufacturer = it) } }
            item { EnabledField("Modelo", profile.model, SpoofFields.MODEL, profile, ::toggleField) { profile = profile.copy(model = it) } }
            item { EnabledField("Codinome do aparelho", profile.deviceCode, SpoofFields.DEVICE, profile, ::toggleField) { profile = profile.copy(deviceCode = it) } }
            item { EnabledField("Produto", profile.productName, SpoofFields.PRODUCT, profile, ::toggleField) { profile = profile.copy(productName = it) } }
            item { EnabledField("Placa", profile.board, SpoofFields.BOARD, profile, ::toggleField) { profile = profile.copy(board = it) } }
            item { EnabledField("Hardware", profile.hardware, SpoofFields.HARDWARE, profile, ::toggleField) { profile = profile.copy(hardware = it) } }
            item { EnabledField("Plataforma", profile.boardPlatform, SpoofFields.PLATFORM, profile, ::toggleField) { profile = profile.copy(boardPlatform = it) } }
            item { SectionTitle("Build e tela", "Android, fingerprint e resolução") }
            item { EnabledField("Versão do Android", profile.buildRelease, SpoofFields.RELEASE, profile, ::toggleField) { profile = profile.copy(buildRelease = it) } }
            item { EnabledField("Nível SDK", profile.buildSdk, SpoofFields.SDK, profile, ::toggleField) { profile = profile.copy(buildSdk = it) } }
            item { EnabledField("Patch de segurança", profile.securityPatch, SpoofFields.PATCH, profile, ::toggleField) { profile = profile.copy(securityPatch = it) } }
            item { EnabledField("Build ID", profile.buildId, SpoofFields.BUILD_ID, profile, ::toggleField) { profile = profile.copy(buildId = it) } }
            item { EnabledField("Display ID", profile.buildDisplayId, SpoofFields.DISPLAY_ID, profile, ::toggleField) { profile = profile.copy(buildDisplayId = it) } }
            item { EnabledField("Incremental", profile.buildIncremental, SpoofFields.INCREMENTAL, profile, ::toggleField) { profile = profile.copy(buildIncremental = it) } }
            item { EnabledField("Fingerprint", profile.buildFingerprint, SpoofFields.FINGERPRINT, profile, ::toggleField, minLines = 3) { profile = profile.copy(buildFingerprint = it) } }
            item {
                EnabledField("Tela (largura x altura x densidade)", "${profile.screenWidth} x ${profile.screenHeight} x ${profile.screenDensity}", SpoofFields.SCREEN, profile, ::toggleField) { value ->
                    val parts = value.split('x').map(String::trim); if (parts.size == 3) profile = profile.copy(screenWidth = parts[0], screenHeight = parts[1], screenDensity = parts[2])
                }
            }
            item { SectionTitle("Rede e região", "Operadora, país, fuso e localidade") }
            item { EnabledField("Operadora", profile.operatorAlpha, SpoofFields.OPERATOR, profile, ::toggleField) { profile = profile.copy(operatorAlpha = it) } }
            item { ProfileField("Código da operadora", profile.operatorNumeric, { profile = profile.copy(operatorNumeric = it) }) }
            item { ProfileField("País SIM", profile.simCountryIso, { profile = profile.copy(simCountryIso = it) }) }
            item { EnabledField("Fuso horário", profile.timezone, SpoofFields.REGION, profile, ::toggleField) { profile = profile.copy(timezone = it) } }
            item { ProfileField("Localidade", profile.locale, { profile = profile.copy(locale = it) }) }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Avançado", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Identificadores deixados vazios preservam o valor real", style = MaterialTheme.typography.bodySmall) }
                    TextButton(onClick = { advancedVisible = !advancedVisible }) { Text(if (advancedVisible) "Ocultar identificadores avançados" else "Mostrar identificadores avançados") }
                }
            }
            if (advancedVisible) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { profile = profile.randomizeAdvanced(); message = "Identificadores gerados no editor; salvar não confirma a aplicação nos apps." }, modifier = Modifier.weight(1f)) { Text("Randomizar tudo") }
                        OutlinedButton(onClick = { profile = profile.randomizeAdvanced() }, modifier = Modifier.weight(1f)) { Text("Gerar em lote") }
                    }
                }
                item {
                    Column(Modifier.fillMaxWidth()) {
                        Text("Campos obrigatórios", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("O perfil não será salvo enquanto um campo obrigatório estiver vazio.", style = MaterialTheme.typography.bodySmall)
                        RequiredToggle("Android ID", SpoofFields.DEVICE_ID, profile) { toggleRequired(SpoofFields.DEVICE_ID, it) }
                        RequiredToggle("MAC Wi‑Fi", SpoofFields.MAC, profile) { toggleRequired(SpoofFields.MAC, it) }
                        RequiredToggle("IMEI", SpoofFields.IMEI, profile) { toggleRequired(SpoofFields.IMEI, it) }
                        RequiredToggle("Exigir todos os identificadores avançados", null, profile) { required ->
                            profile = profile.copy(requiredFields = if (required) SpoofFields.IDENTIFIER_FIELDS else emptySet())
                        }
                    }
                }
                item { AdvancedField("Device ID / Android ID", profile.deviceId, SpoofFields.DEVICE_ID, profile, ::toggleField, { profile = profile.copy(deviceId = it) }) { profile = profile.randomizeAdvanced().copy(macAddress = profile.macAddress, imei = profile.imei, meid = profile.meid, imsi = profile.imsi, iccid = profile.iccid, phoneNumber = profile.phoneNumber, advertisingId = profile.advertisingId, gsfId = profile.gsfId, mediaDrmId = profile.mediaDrmId, appSetId = profile.appSetId, serial = profile.serial) } }
                item { AdvancedField("MAC Wi‑Fi", profile.macAddress, SpoofFields.MAC, profile, ::toggleField, { profile = profile.copy(macAddress = it) }) { profile = profile.randomizeAdvanced().copy(deviceId = profile.deviceId, imei = profile.imei, meid = profile.meid, imsi = profile.imsi, iccid = profile.iccid, phoneNumber = profile.phoneNumber, advertisingId = profile.advertisingId, gsfId = profile.gsfId, mediaDrmId = profile.mediaDrmId, appSetId = profile.appSetId, serial = profile.serial) } }
                item { AdvancedField("IMEI", profile.imei, SpoofFields.IMEI, profile, ::toggleField, { profile = profile.copy(imei = it) }) { profile = profile.randomizeAdvanced().copy(macAddress = profile.macAddress, deviceId = profile.deviceId, meid = profile.meid, imsi = profile.imsi, iccid = profile.iccid, phoneNumber = profile.phoneNumber, advertisingId = profile.advertisingId, gsfId = profile.gsfId, mediaDrmId = profile.mediaDrmId, appSetId = profile.appSetId, serial = profile.serial) } }
                item { AdvancedField("MEID", profile.meid, SpoofFields.MEID, profile, ::toggleField, { profile = profile.copy(meid = it) }) { profile = profile.randomizeAdvanced().copy(macAddress = profile.macAddress, deviceId = profile.deviceId, imei = profile.imei, imsi = profile.imsi, iccid = profile.iccid, phoneNumber = profile.phoneNumber, advertisingId = profile.advertisingId, gsfId = profile.gsfId, mediaDrmId = profile.mediaDrmId, appSetId = profile.appSetId, serial = profile.serial) } }
                item { AdvancedField("IMSI", profile.imsi, SpoofFields.IMSI, profile, ::toggleField, { profile = profile.copy(imsi = it) }) { profile = profile.randomizeAdvanced().copy(macAddress = profile.macAddress, deviceId = profile.deviceId, imei = profile.imei, meid = profile.meid, iccid = profile.iccid, phoneNumber = profile.phoneNumber, advertisingId = profile.advertisingId, gsfId = profile.gsfId, mediaDrmId = profile.mediaDrmId, appSetId = profile.appSetId, serial = profile.serial) } }
                item { AdvancedField("ICCID", profile.iccid, SpoofFields.ICCID, profile, ::toggleField, { profile = profile.copy(iccid = it) }) { profile = profile.randomizeAdvanced().copy(macAddress = profile.macAddress, deviceId = profile.deviceId, imei = profile.imei, meid = profile.meid, imsi = profile.imsi, phoneNumber = profile.phoneNumber, advertisingId = profile.advertisingId, gsfId = profile.gsfId, mediaDrmId = profile.mediaDrmId, appSetId = profile.appSetId, serial = profile.serial) } }
                item { AdvancedField("Telefone", profile.phoneNumber, SpoofFields.PHONE, profile, ::toggleField, { profile = profile.copy(phoneNumber = it) }) { profile = profile.randomizeAdvanced().copy(macAddress = profile.macAddress, deviceId = profile.deviceId, imei = profile.imei, meid = profile.meid, imsi = profile.imsi, iccid = profile.iccid, advertisingId = profile.advertisingId, gsfId = profile.gsfId, mediaDrmId = profile.mediaDrmId, appSetId = profile.appSetId, serial = profile.serial) } }
                item { AdvancedField("Google Advertising ID", profile.advertisingId, SpoofFields.AD_ID, profile, ::toggleField, { profile = profile.copy(advertisingId = it) }) { profile = profile.randomizeAdvanced().copy(macAddress = profile.macAddress, deviceId = profile.deviceId, imei = profile.imei, meid = profile.meid, imsi = profile.imsi, iccid = profile.iccid, phoneNumber = profile.phoneNumber, gsfId = profile.gsfId, mediaDrmId = profile.mediaDrmId, appSetId = profile.appSetId, serial = profile.serial) } }
                item { AdvancedField("GSF ID", profile.gsfId, SpoofFields.GSF_ID, profile, ::toggleField, { profile = profile.copy(gsfId = it) }) { profile = profile.randomizeAdvanced().copy(macAddress = profile.macAddress, deviceId = profile.deviceId, imei = profile.imei, meid = profile.meid, imsi = profile.imsi, iccid = profile.iccid, phoneNumber = profile.phoneNumber, advertisingId = profile.advertisingId, mediaDrmId = profile.mediaDrmId, appSetId = profile.appSetId, serial = profile.serial) } }
                item { AdvancedField("MediaDrm ID (hex)", profile.mediaDrmId, SpoofFields.DRM_ID, profile, ::toggleField, { profile = profile.copy(mediaDrmId = it) }, minLines = 2) { profile = profile.randomizeAdvanced().copy(macAddress = profile.macAddress, deviceId = profile.deviceId, imei = profile.imei, meid = profile.meid, imsi = profile.imsi, iccid = profile.iccid, phoneNumber = profile.phoneNumber, advertisingId = profile.advertisingId, gsfId = profile.gsfId, appSetId = profile.appSetId, serial = profile.serial) } }
                item { AdvancedField("App Set ID", profile.appSetId, SpoofFields.APP_SET_ID, profile, ::toggleField, { profile = profile.copy(appSetId = it) }) { profile = profile.randomizeAdvanced().copy(macAddress = profile.macAddress, deviceId = profile.deviceId, imei = profile.imei, meid = profile.meid, imsi = profile.imsi, iccid = profile.iccid, phoneNumber = profile.phoneNumber, advertisingId = profile.advertisingId, gsfId = profile.gsfId, mediaDrmId = profile.mediaDrmId, serial = profile.serial) } }
                item { AdvancedField("Serial", profile.serial, SpoofFields.SERIAL, profile, ::toggleField, { profile = profile.copy(serial = it) }) { profile = profile.randomizeAdvanced().copy(macAddress = profile.macAddress, deviceId = profile.deviceId, imei = profile.imei, meid = profile.meid, imsi = profile.imsi, iccid = profile.iccid, phoneNumber = profile.phoneNumber, advertisingId = profile.advertisingId, gsfId = profile.gsfId, mediaDrmId = profile.mediaDrmId, appSetId = profile.appSetId) } }
            }
            item { SectionTitle("Apps deste perfil", "Marque os apps deste usuário Android. No LSPosed, abra o gerenciador dentro do Work Profile (ícone de maleta) e selecione o mesmo pacote nesse usuário.") }
            item { ProfileField("Buscar app", appSearch, { appSearch = it }) }
            val visibleApps = installedApps.filter { appSearch.isBlank() || it.label.contains(appSearch, true) || it.packageName.contains(appSearch, true) }
            items(visibleApps, key = { it.packageName }) { app ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = app.packageName in enabledPackages, onCheckedChange = { checked -> repository.setPackageEnabled(app.packageName, checked); enabledPackages = repository.enabledPackages() })
                    Column(Modifier.weight(1f)) { Text(app.label); Text(app.packageName, style = MaterialTheme.typography.bodySmall) }
                }
                HorizontalDivider()
            }
            item { Button(onClick = ::save, modifier = Modifier.fillMaxWidth()) { Text("Salvar perfil WorkSpoof") } }
            item { Text("Módulo experimental, ainda sem teste em aparelho real. O escopo do LSPosed é separado por usuário Android: a seleção pessoal não ativa o app do Work Profile. GSF ID e App Set ID são somente armazenados: não há hooks implementados para eles. Demais campos também dependem da API usada pelo aplicativo. Não altera o modem, o SIM ou a identidade física do aparelho.", style = MaterialTheme.typography.bodySmall) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(Modifier.padding(top = 8.dp)) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(subtitle, style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun ProfileField(label: String, value: String, onChange: (String) -> Unit, minLines: Int = 1) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), minLines = minLines)
}

@Composable
private fun EnabledField(label: String, value: String, key: String, profile: SpoofProfile, toggle: (String, Boolean) -> Unit, minLines: Int = 1, onChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = key in profile.enabledFields, onCheckedChange = { toggle(key, it) })
        OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, modifier = Modifier.weight(1f), minLines = minLines)
    }
}

@Composable
private fun AdvancedField(label: String, value: String, key: String, profile: SpoofProfile, toggle: (String, Boolean) -> Unit, onChange: (String) -> Unit, minLines: Int = 1, randomize: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = key in profile.enabledFields, onCheckedChange = { toggle(key, it) })
        OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, modifier = Modifier.weight(1f), minLines = minLines)
        TextButton(onClick = randomize) { Text("🎲") }
    }
}

@Composable
private fun RequiredToggle(label: String, key: String?, profile: SpoofProfile, onChange: (Boolean) -> Unit) {
    val checked = key?.let { it in profile.requiredFields } ?: profile.requiredFields.containsAll(SpoofFields.IDENTIFIER_FIELDS)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
