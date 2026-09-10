package com.monstera.harbor.spoof

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit

/** No startup requests, global property changes, APK rewriting or shell interpolation. */
object RootAccess {
    suspend fun request(): String = withContext(Dispatchers.IO) {
        var process: Process? = null
        try {
            val child = ProcessBuilder("su", "-c", "id -u").redirectErrorStream(true).start()
            process = child
            if (!child.waitFor(30, TimeUnit.SECONDS)) {
                "Tempo esgotado. Nenhuma alteração foi feita; tente novamente se desejar."
            } else {
                val output = child.inputStream.bufferedReader().use { it.readText().trim() }
                if (child.exitValue() == 0 && output.lineSequence().any { it == "0" }) {
                    "Root concedido nesta verificação (UID 0). Isso não confirma LSPosed nem spoof ativo."
                } else "Root negado ou indisponível. O perfil de trabalho continua disponível sem root."
            }
        } catch (_: IOException) {
            "Comando su indisponível. Use o modo sem root ou configure seu gerenciador de root."
        } finally {
            process?.let { if (it.isAlive) it.destroyForcibly() }
        }
    }
}

@Composable
fun RootSettingsCard() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("workspof_root", Context.MODE_PRIVATE) }
    var enabled by remember { mutableStateOf(prefs.getBoolean("enabled", false)) }
    var verified by remember { mutableStateOf(prefs.getBoolean("verified", false)) }
    var busy by remember { mutableStateOf(false) }
    var status by remember {
        mutableStateOf(
            if (enabled && verified) "Root autorizado anteriormente. A permissão é controlada pelo seu gerenciador de root."
            else "Root não verificado. Nenhuma solicitação automática.",
        )
    }
    val scope = rememberCoroutineScope()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Root e LSPosed · opcional", style = MaterialTheme.typography.titleMedium)
            Text("Sem NPatch. Os APKs e as assinaturas dos apps selecionados permanecem intactos.")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Habilitar opções de root", modifier = Modifier.weight(1f))
                Switch(checked = enabled, enabled = !busy, onCheckedChange = {
                    enabled = it
                    prefs.edit().putBoolean("enabled", it).apply()
                    if (!it) {
                        verified = false
                        prefs.edit().putBoolean("verified", false).apply()
                        status = "Opções de root desligadas; a permissão pode ser revogada no gerenciador de root."
                    } else if (verified) {
                        status = "Root autorizado anteriormente. A permissão é controlada pelo seu gerenciador de root."
                    }
                })
            }
            Button(enabled = enabled && !busy, onClick = {
                busy = true
                scope.launch {
                    try {
                        val result = RootAccess.request()
                        val granted = result.startsWith("Root concedido")
                        verified = granted
                        prefs.edit().putBoolean("verified", granted).apply()
                        status = result
                    } finally { busy = false }
                }
            }) { Text(if (busy) "Aguardando autorização…" else if (verified) "Verificar root novamente" else "Solicitar / verificar root") }
            Text(status, style = MaterialTheme.typography.bodySmall)
            Text("Para spoof: instale LSPosed compatível, habilite o módulo WorkSpoof e marque os apps deste perfil no escopo. Salve a identidade e reabra os apps. Root sozinho não substitui identificadores.", style = MaterialTheme.typography.bodySmall)
            Text("O estado autorizado fica salvo até você desligar esta opção. O Android/gerenciador de root ainda pode revogar a permissão; este botão não mantém um processo su aberto.", style = MaterialTheme.typography.bodySmall)
            Text("Este botão apenas executa id -u. Não instala módulos, não altera propriedades globais e não remove verificações de root dos apps.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
