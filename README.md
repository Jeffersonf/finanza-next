# Finanza Next

Um aplicativo Android nativo para os dados e regras do Finanza. A base funcional é única: login, sincronização, transações, contas, planejamento, módulos pessoais e espaços compartilhados são os mesmos em qualquer visual.

O app oferece dois temas completos, sem separar dados ou navegação em aplicativos diferentes:

- **Next:** linguagem escura, direta e compacta.
- **Finanza:** linguagem visual do Finanza web, com superfícies grafite translúcidas, lime, teal, roxo e navegação inferior inspirada na versão mobile do web.

O tema é apenas uma camada de apresentação. Alterá-lo não altera conta, sessão, dados ou funcionalidades.

## Web

O app estático é servido diretamente da raiz do repositório e publicado pelo GitHub Pages.

Para testar localmente:

```powershell
python -m http.server 4179 --bind 127.0.0.1
```

Depois acesse `http://127.0.0.1:4179/`.

## Android nativo

O projeto Kotlin fica em `android-kotlin` e usa Android SDK 36, Java 17 e minSdk 26.

```powershell
cd android-kotlin
.\gradlew.bat assembleDebug
```

O APK de desenvolvimento é gerado em `android-kotlin/app/build/outputs/apk/debug/app-debug.apk`.
