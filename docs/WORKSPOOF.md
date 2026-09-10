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

## Aplicação opcional por app

O APK também contém um módulo LSPosed opcional. Quando o usuário instala e
habilita esse módulo e seleciona os mesmos apps no escopo do LSPosed, ele pode
interceptar valores em processos dos apps selecionados, sem modificar seus
APKs nem suas assinaturas. A configuração continua isolada dentro de cada
usuário Android.

Cobertura atual do módulo: `Build`, tela, Android ID, IMEI/MEID/IMSI/ICCID,
número de telefone, operadora, MAC Wi‑Fi/Bluetooth, serial, MediaDrm e o
Advertising ID clássico. GSF ID e App Set ID dependem de APIs internas do
Google Play Services e não são prometidos para todas as versões.

Use perfis de teste e identificadores sintéticos. Não use o recurso para se
passar por outra pessoa ou contornar controles de serviços.
