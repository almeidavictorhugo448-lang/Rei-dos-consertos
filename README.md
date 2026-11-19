# Rei dos Consertos

Jogo de simulação de uma assistência técnica, feito para Android.

## Como jogar

1. **Diagnóstico:** leia o pedido e escolha a peça que pode estar com defeito.
2. **Peças:** consulte o estoque e selecione o componente necessário.
3. **Loja:** compre componentes quando o estoque estiver vazio.
4. **Instalar:** remova a peça antiga e instale a peça correta.
5. **Testar:** faça os testes antes de entregar o aparelho.
6. **Entregar:** receba o pagamento e aumente a reputação da assistência.

O jogo inclui serviços de PC gamer, celular, notebook, Nintendo Switch, PlayStation, Xbox e controles Joy-Con.

## Progresso

Dinheiro, reputação, dia, quantidade de serviços e estoque são salvos automaticamente no aparelho usando SharedPreferences.

## Gerar o APK

O projeto usa Android Gradle Plugin 8.7.3 e Java 17.

Pelo GitHub:
- Abra a aba **Actions**.
- Execute o workflow **Build Android APK** ou aguarde a compilação após um push.
- Baixe o artefato **Rei-dos-Consertos-APK** quando o workflow terminar.

Localmente, com Android SDK instalado:

```bash
gradle assembleDebug
```

O APK de depuração será gerado em `app/build/outputs/apk/debug/app-debug.apk`.
