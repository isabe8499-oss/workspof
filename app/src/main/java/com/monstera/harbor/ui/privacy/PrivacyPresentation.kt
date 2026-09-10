package com.monstera.harbor.ui.privacy

import com.monstera.harbor.core.data.ManagedApp
import com.monstera.harbor.core.policy.ManagedProfileProvisioningCapability
import com.monstera.harbor.ui.designsystem.HarborIconKind
import com.monstera.harbor.ui.designsystem.PrivacyFact
import com.monstera.harbor.ui.designsystem.StatusTone

data class WorkSpacePresentation(
    val title: String,
    val body: String,
    val tone: StatusTone,
)

internal enum class PersonalHeroAction {
    OPEN_WORK,
    PROVISION_WORK,
    SEND_FILES,
    ADVANCED,
}

internal data class PersonalHeroActions(
    val primary: PersonalHeroAction?,
    val quickLeft: PersonalHeroAction?,
    val quickRight: PersonalHeroAction?,
)

internal fun personalHeroActions(
    harborManagedProfile: Boolean,
    foreignProfile: Boolean,
    provisioningCapability: ManagedProfileProvisioningCapability,
): PersonalHeroActions = when {
    harborManagedProfile -> PersonalHeroActions(
        primary = PersonalHeroAction.OPEN_WORK,
        quickLeft = PersonalHeroAction.SEND_FILES,
        quickRight = PersonalHeroAction.ADVANCED,
    )
    foreignProfile && provisioningCapability is ManagedProfileProvisioningCapability.Allowed -> PersonalHeroActions(
        primary = PersonalHeroAction.PROVISION_WORK,
        quickLeft = null,
        quickRight = PersonalHeroAction.ADVANCED,
    )
    !foreignProfile && provisioningCapability is ManagedProfileProvisioningCapability.Allowed -> PersonalHeroActions(
        primary = PersonalHeroAction.PROVISION_WORK,
        quickLeft = null,
        quickRight = PersonalHeroAction.ADVANCED,
    )
    else -> PersonalHeroActions(
        primary = null,
        quickLeft = null,
        quickRight = PersonalHeroAction.ADVANCED,
    )
}

fun workSpacePresentation(
    harborManagedProfile: Boolean,
    foreignProfile: Boolean,
    provisioningCapability: ManagedProfileProvisioningCapability,
): WorkSpacePresentation = when {
    harborManagedProfile -> WorkSpacePresentation(
        title = "Perfil de trabalho pronto",
        body = "Seus apps e dados de trabalho ficam separados dos apps pessoais.",
        tone = StatusTone.Positive,
    )
    foreignProfile && provisioningCapability is ManagedProfileProvisioningCapability.Allowed -> WorkSpacePresentation(
        title = "Configuração do perfil de trabalho disponível",
        body = "Já existe outro perfil, mas o WorkSpoof não o gerencia.",
        tone = StatusTone.Warning,
    )
    foreignProfile -> WorkSpacePresentation(
        title = "Configuração do perfil de trabalho indisponível",
        body = "O Android possui outro perfil e não permite que o WorkSpoof crie mais um agora.",
        tone = StatusTone.Warning,
    )
    provisioningCapability is ManagedProfileProvisioningCapability.Allowed -> WorkSpacePresentation(
        title = "Configure seu perfil de trabalho",
        body = "Mantenha os apps escolhidos e seus dados separados dos apps pessoais.",
        tone = StatusTone.Neutral,
    )
    else -> WorkSpacePresentation(
        title = "Configuração do perfil de trabalho indisponível",
        body = "O estado atual de gerenciamento do Android impede o WorkSpoof de criar o perfil de trabalho.",
        tone = StatusTone.Warning,
    )
}

fun privacyFacts(): List<PrivacyFact> = listOf(
    PrivacyFact("Sem permissão de rede", "O WorkSpoof não acessa a internet.", HarborIconKind.Network),
    PrivacyFact("Sem analytics", "O WorkSpoof não coleta uso nem telemetria.", HarborIconKind.Analytics),
    PrivacyFact("Somente local", "O catálogo e os diagnósticos ficam neste aparelho.", HarborIconKind.Device),
)

fun appStatusLabel(app: ManagedApp): String = when {
    app.isHidden -> "Frozen"
    app.isSystem -> "Read-only"
    !app.isEnabled -> "Desativado"
    else -> "Available"
}

fun appStatusTone(app: ManagedApp): StatusTone = when {
    app.isHidden -> StatusTone.Neutral
    app.isSystem -> StatusTone.Warning
    !app.isEnabled -> StatusTone.Warning
    else -> StatusTone.Positive
}
