# Experimento final

## Plano

| Item | Valor |
|---|---|
| Eb/N0 | −2 a 12 dB, passo 2 dB (8 valores) |
| Bits por execução | 10⁶ |
| Sementes | 5 (1 a 5) |
| Esquemas | BPSK, QPSK, 16-QAM, AMI, Manchester (5) |
| Canais | AWGN, rajadas (p = 0,01, r = 0,1, fator 2,0) |
| Códigos de canal | nenhum, Hamming(7,4) |
| Execuções | 5 × 2 × 2 × 8 × 5 = **800** |

Configuração: `config/experimento_base.properties`.

Comando: `bash scripts/executar_tudo.sh`.

Saídas: `resultados/brutos/experimento_base.csv` e `resultados/logs/`.

## Justificativa de 10⁶ bits por execução

Uma BER estimada por contagem de erros só é confiável quando há erros suficientes. A contagem segue aproximadamente uma distribuição de Poisson, então o erro relativo da estimativa é cerca de 1/√(erros). Para ficar perto de 10%, são necessários cerca de 100 erros.

- Com 10⁶ bits por execução e 5 sementes, cada ponto do gráfico tem 5·10⁶ bits. Isso dá cerca de 100 erros quando a BER é 2·10⁻⁵.
- No piloto (10⁵ bits, 2 sementes, 2·10⁵ bits por ponto), BPSK sem código em 10 dB (BER teórica 1,5·10⁻⁵) teve só 3 erros. A estimativa ali é muito ruidosa. Com o experimento final, o mesmo ponto terá cerca de 75 erros.
- 10⁶ bits é o mesmo tamanho de bloco usado nos testes de validação teórica, que bateram com a teoria dentro de ±15%.
- 10⁷ bits por execução multiplicaria o tempo por 10 sem mudar a conclusão nos pontos onde a BER já é mensurável, e cabe mal na memória (os sinais são arrays de `Complex`).

**Limite que precisa ser declarado.** Em 12 dB, BPSK, QPSK e Manchester no AWGN têm BER teórica abaixo de 10⁻⁷. Mesmo com 5·10⁶ bits, o esperado é 0 erro. Nesses pontos, o gráfico em escala logarítmica não pode mostrar BER = 0. A convenção adotada é plotar o limite superior de 3/N ≈ 6·10⁻⁷ (regra dos três) e marcá-lo como "< 6·10⁻⁷", ou omitir o ponto e dizer isso na legenda.

**Intervalo de confiança.** Com 5 sementes, t(4 gl) = 2,776. O IC 95% é calculado por `Statistics.resumir` sobre as 5 BER por ponto.

## Tempo de execução

- Piloto (320 execuções, 10⁵ bits): [preencher com o tempo registrado no log].
- Estimativa do final: 25 × tempo do piloto.
- Tempo real do experimento final: **242,8 segundos (aproximadamente 4 minutos e 3 segundos)**.

A execução final começou em 17/11/2026 às 23:18:16 e foi concluída com sucesso, totalizando as 800 execuções.

## Testes e validação

Antes da execução final, foram realizados testes para verificar o funcionamento do script e o comportamento do experimento em diferentes situações.

### Teste com configuração reduzida

Foi executado um teste utilizando uma configuração menor, com 32 execuções, para verificar o funcionamento do script sem precisar executar todo o experimento.

Comando utilizado:

```bash
ESPERADO=33 CONFIG=config/experimento_teste.properties SAIDA=resultados/teste/script_teste.csv bash scripts/executar_tudo.sh
```

O teste foi concluído com sucesso em aproximadamente 0,3 segundo. O arquivo CSV gerado apresentou 33 linhas, sendo uma linha de cabeçalho e 32 registros de resultados.

Esse teste permitiu verificar a leitura da configuração, a execução das simulações, a gravação dos resultados e o acompanhamento do progresso pelo script.

### Execução completa do experimento

Após a validação inicial, foi realizada a execução completa utilizando a configuração `config/experimento_base.properties`.

A execução foi concluída com sucesso, totalizando **800 execuções**, conforme o planejamento. O tempo registrado foi de aproximadamente 242,8 segundos.

O arquivo gerado foi `resultados/brutos/experimento_base.csv`, contendo 800 registros de resultados, além do cabeçalho. A execução também gerou um arquivo de log com informações sobre o ambiente e o andamento do experimento.

### Testes automatizados

Também foi executado o comando:

```bash
mvn test
```

O resultado foi:

- Testes executados: **204**
- Falhas: **0**
- Erros: **0**
- Testes ignorados: **0**
- Resultado: **BUILD SUCCESS**

Os testes abrangeram diferentes partes do simulador, incluindo canais, modulação, codificação, transmissão, métricas, configurações e geração dos resultados.

Entre as classes de teste executadas estão `ConfigLoaderTest`, `ExperimentRunnerTest`, `ResultWriterTest` e `StatisticsTest`, que verificam, respectivamente, a leitura das configurações, a execução dos experimentos, a gravação dos resultados e os cálculos estatísticos.

### Teste de configuração inválida

Também foi verificado o comportamento do script ao receber um arquivo de configuração inexistente.

Comando utilizado:

```bash
CONFIG=config/arquivo_inexistente.properties bash scripts/executar_tudo.sh
```

O script identificou corretamente o problema e apresentou a mensagem:

```text
ERRO: configuração não encontrada: config/arquivo_inexistente.properties
```

Esse teste confirmou que o script verifica a existência do arquivo de configuração antes de iniciar o experimento.

## Reprodutibilidade

- Mesma configuração e mesmas sementes geram o mesmo CSV (verificado por `ExperimentRunnerTest`).
- O log guarda a versão do Java, o commit e as datas de início e fim.
- A configuração usada é copiada para `resultados/brutos/`.
- O CSV é gravado em `.parcial` e renomeado só no fim. Se a execução for interrompida, não fica um CSV incompleto com cara de final.

## Considerações finais

O experimento final foi executado conforme o planejamento, totalizando 800 execuções e aproximadamente 242,8 segundos de processamento. Os testes automatizados também foram concluídos com sucesso, sem falhas ou erros.

Os testes com configuração reduzida e configuração inválida demonstraram que o script responde corretamente tanto em situações normais quanto diante de problemas na configuração. Dessa forma, os resultados obtidos e as verificações realizadas fornecem evidências do funcionamento do simulador e da execução do experimento planejado.