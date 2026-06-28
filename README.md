# Finanza Next

Nova experiência visual do Finanza, mantida em um projeto independente para evoluir sem alterar o Finanza principal.

O Next preserva conta, sincronização e recursos financeiros existentes, mas possui navegação, hierarquia, componentes e ritmo visual próprios. A interface segue uma linguagem mobile-first de alto contraste: fundo cinza-claro, cartões brancos, um cartão preto dominante e cor de progresso configurável.

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
