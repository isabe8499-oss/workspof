# WorkSpoof: modos de funcionamento

WorkSpoof é um fork do Harbor. Ele usa as APIs oficiais do Android para criar
e gerir um perfil de trabalho, portanto estas funções funcionam sem root:

- criar e abrir o Work Profile;
- isolar apps, dados e armazenamento do perfil pessoal;
- congelar, abrir, desinstalar e gerir os apps do perfil;
- importar, editar, validar e exportar um perfil JSON de aparelho;
- escolher campos obrigatórios (Android ID, MAC, IMEI e outros);
- escolher, por perfil Android, quais apps terão uma identidade virtual
  configurada.

## Limite do modo sem root

Android não oferece uma API pública para um app trocar IMEI, IMSI, ICCID, MAC,
MediaDrm, `Build.*` ou Android ID retornados a outro aplicativo. Work Profile
não elimina essa limitação. No modo sem root, WorkSpoof mantém a identidade
virtual, valida os campos obrigatórios e isola os dados; ele não afirma que
esses valores foram injetados em outros apps.

O Android já separa os dados de cada usuário/perfil, e o Android ID é escopado
por usuário e assinatura desde Android 8. Isso fornece isolamento real mesmo
sem root, mas não equivale a substituir identificadores de hardware.

## O que o Work Profile não esconde

O Work Profile é uma separação de dados e aplicativos, não uma camada de
anonimato contra o próprio sistema. Um app dentro dele ainda pode consultar ou
inferir sinais de root, Magisk, bootloader desbloqueado, hooks Xposed/LSPosed,
Play Integrity e estado do dispositivo. O módulo WorkSpoof não remove esses
sinais e não promete passar por verificações anti-root ou anti-hook.

## Aplicação opcional por app

O APK também contém um módulo LSPosed opcional. Quando o usuário instala e
habilita esse módulo e seleciona os mesmos apps no escopo do LSPosed, ele pode
interceptar valores em processos dos apps selecionados, sem modificar seus
APKs nem suas assinaturas. A configuração continua isolada dentro de cada
usuário Android.

O módulo é experimental e ainda não foi validado em aparelho. Existem hooks
para algumas APIs Java de `Build`, tela, Android ID, IMEI/MEID/IMSI/ICCID,
número de telefone, operadora, MAC Wi‑Fi/Bluetooth, serial, MediaDrm e
Advertising ID clássico. Outras APIs, caches ou código nativo podem continuar
retornando valores reais. GSF ID e App Set ID são apenas armazenados no editor:
não há hooks implementados para eles. Nenhum identificador físico de modem,
SIM ou interface de rede é alterado.

## Opção de root nas configurações

Em **Configurações → Spoof do aparelho → Root e LSPosed**, habilite as opções
de root e toque em **Solicitar / verificar root**. Depois de uma verificação
concedida, o estado fica salvo até você desligar a opção. O botão executa apenas
`su -c 'id -u'`, mediante autorização no gerenciador de root, com limite de
30 segundos. Não há solicitação automática ao abrir o app.

Essa verificação não instala LSPosed nem confirma que os hooks estão ativos.
Root sozinho não aplica spoof. Não é necessário NPatch ou reempacotar os apps
selecionados. Desligar a opção oculta/desabilita a solicitação de root; para
revogar uma permissão já concedida, use o gerenciador de root. Para desligar
os hooks, desative a identidade virtual e reabra os apps, ou desative o módulo
no LSPosed.

## Apps do Work Profile no LSPosed

O WorkSpoof salva o perfil e os pacotes no usuário Android em que foi aberto.
Para aplicar em um app com a maleta, abra o WorkSpoof e o LSPosed pelo usuário
de trabalho. O LSPosed Manager instalado no perfil pessoal pode listar somente
os pacotes do usuário pessoal; nesse caso, instale/clone o Manager e o módulo
WorkSpoof no Work Profile e selecione o pacote com a maleta no escopo. Depois
de salvar, reinicie o processo do app selecionado.

Use perfis de teste e identificadores sintéticos. Não use o recurso para se
passar por outra pessoa ou contornar controles de serviços.
