# Next Android - Device QA

Use este checklist no aparelho ou emulador assim que o APK estiver instalado.

## 1. Launcher

- Verifique se o icone novo aparece na home e no drawer.
- Faça long press no icone do app.
- Confirme que o shortcut `Novo gasto` aparece.
- Toque no shortcut e confirme que abre direto a folha rapida.

## 2. Folha rapida

- No app, toque em `+` e confirme que abre o formulario normal de gasto.
- Dentro do formulario normal, use `Usar folha rapida` para testar a captura compacta.
- Abra a folha rapida pelo shortcut do launcher.
- Abra a folha rapida pelo widget rapido.
- Abra a folha rapida pela notificacao persistente.
- Em todos os casos, confirme:
  - a folha abre no topo, pequena e translucida
  - nao parece uma tela cheia preta
  - nao invade a barra superior
  - o teclado abre automaticamente
  - a etapa `1 Valor` aparece ativa primeiro
  - depois de `OK`, a etapa `2 Descricao` fica ativa
  - salvar fecha a folha e grava o gasto

## 3. Widget rapido

- Adicione o widget rapido na home.
- Confirme que ele esta pequeno e translucido.
- Confirme que mostra a conta principal.
- Toque em `Digitar`.
- Verifique se abre a mesma folha rapida do app.

## 4. Notificacao persistente

- Confirme que a notificacao persistente aparece.
- Toque em `Folha` e confirme a abertura da folha rapida.
- Use `Digitar` com algo como `34 mercado`.
- Confirme que o gasto entra no feed.

## 5. Fluxo normal do app

- Home: conferir hero, cards e acoes rapidas.
- Movimentos: conferir filtros, busca e edicao de item.
- Contas: criar conta, editar conta e pagar compromisso.
- Ajustes: abrir tela de conta online e tema.

## 6. Sincronizacao

- Entrar com conta web.
- Confirmar que o estado conectado mostra nome e host.
- Sincronizar e verificar se movimentos/contas aparecem no app.

## 7. Regressao visual

- Tema claro.
- Tema escuro.
- Verificar se textos nao sobrepoem.
- Verificar se botoes e pills mantem o mesmo estilo glass.
