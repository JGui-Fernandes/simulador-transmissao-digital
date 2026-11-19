#!/usr/bin/env bash
# scripts/executar_tudo.sh
# Reproduz todos os resultados do zero: compila, roda o experimento, confere o CSV,
# gera as figuras e a tabela de compromisso.
# Uso (a partir de qualquer pasta):  bash scripts/executar_tudo.sh
# Variáveis opcionais:
#   CONFIG=config/experimento_base.properties   configuração a executar
#   SAIDA=resultados/brutos/experimento_base.csv
#   JAR=caminho/do/gb.jar                        pula a detecção do jar
#   JAVA_OPTS="-Xmx2g"                           opções da JVM
#   PULAR_BUILD=1                                usa o jar já existente
#   FIGURAS=docs/figuras                         pasta das figuras
#   TABELA=docs/tabela_compromisso.md            arquivo da tabela de compromisso
# Com uma SAIDA diferente da padrão (ex.: teste), figuras e tabela vão para a pasta
# do CSV, para não sobrescrever os resultados oficiais em docs/.

set -euo pipefail

# 1. Ir para a raiz do projeto (a pasta acima de scripts/), não importa de onde o script foi chamado
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ"

CONFIG="${CONFIG:-config/experimento_base.properties}"
SAIDA="${SAIDA:-resultados/brutos/experimento_base.csv}"
JAVA_OPTS="${JAVA_OPTS:--Xmx2g}"
SAIDA_PADRAO="resultados/brutos/experimento_base.csv"
if [ "$SAIDA" = "$SAIDA_PADRAO" ]; then
    FIGURAS="${FIGURAS:-docs/figuras}"
    TABELA="${TABELA:-docs/tabela_compromisso.md}"
else
    FIGURAS="${FIGURAS:-$(dirname "$SAIDA")/figuras}"
    TABELA="${TABELA:-$(dirname "$SAIDA")/tabela_compromisso.md}"
fi
LOGS="resultados/logs"
CARIMBO="$(date +%Y%m%d_%H%M%S)"
LOG="$LOGS/experimento_base_$CARIMBO.log"

mkdir -p "$(dirname "$SAIDA")" "$LOGS"

# Tudo o que o script imprime também vai para o log
exec > >(tee -a "$LOG") 2>&1

echo "=== Reprodução completa: experimento, figuras e tabela ==="
echo "Início:   $(date '+%Y-%m-%d %H:%M:%S')"
echo "Config:   $CONFIG"
echo "Saída:    $SAIDA"
echo "Figuras:  $FIGURAS"
echo "Tabela:   $TABELA"
echo "Log:      $LOG"
command -v git >/dev/null 2>&1 && git rev-parse --short HEAD 2>/dev/null | sed 's/^/Commit:   /' || true
java -version 2>&1 | sed 's/^/Java:     /'

# 2. Conferir pré-requisitos
[ -f "$CONFIG" ] || { echo "ERRO: configuração não encontrada: $CONFIG"; exit 2; }
command -v java >/dev/null 2>&1 || { echo "ERRO: java não encontrado no PATH"; exit 2; }

# 3. Compilar (a menos que PULAR_BUILD=1)
if [ -z "${PULAR_BUILD:-}" ] && [ -z "${JAR:-}" ]; then
    echo "--- Compilando ---"
    if [ -f pom.xml ]; then
        mvn -q -DskipTests clean package
    elif [ -x ./gradlew ]; then
        ./gradlew --no-daemon clean jar
    elif [ -f build.gradle ] || [ -f build.gradle.kts ]; then
        gradle --no-daemon clean jar
    else
        echo "ERRO: nem pom.xml nem build.gradle encontrados"; exit 2
    fi
fi

# 4. Localizar o jar
if [ -z "${JAR:-}" ]; then
    for candidato in target/gb.jar build/libs/gb.jar; do
        if [ -f "$candidato" ]; then JAR="$candidato"; break; fi
    done
fi
[ -n "${JAR:-}" ] && [ -f "$JAR" ] || { echo "ERRO: gb.jar não encontrado (defina JAR=...)"; exit 2; }
echo "Jar:      $JAR"

# 5. Guardar uma cópia da configuração usada junto aos resultados
cp "$CONFIG" "$(dirname "$SAIDA")/$(basename "$CONFIG")"

# 6. Rodar o experimento. O ExperimentRunner tem o seu próprio main, por isso -cp.
echo "--- Executando (800 execuções esperadas) ---"
INICIO=$(date +%s)
# shellcheck disable=SC2086
java $JAVA_OPTS -cp "$JAR" br.unisinos.edu.experimento.ExperimentRunner "$CONFIG" "$SAIDA"
FIM=$(date +%s)
echo "Tempo total: $((FIM - INICIO)) s"

# 7. Conferência mínima do CSV: cabeçalho + 800 linhas, e nenhum .parcial sobrando
ESPERADO="${ESPERADO:-801}"
OBTIDO=$(wc -l < "$SAIDA" | tr -d ' ')
if [ "$OBTIDO" -ne "$ESPERADO" ]; then
    echo "FALHOU: $SAIDA tem $OBTIDO linhas (esperado $ESPERADO)"; exit 1
fi
if [ -e "$SAIDA.parcial" ]; then
    echo "FALHOU: sobrou $SAIDA.parcial"; exit 1
fi
echo "OK: $SAIDA com $((OBTIDO - 1)) execuções."

# 8. Figuras (Etapa 11): lê o CSV e grava os PNGs
echo "--- Gerando figuras ---"
mkdir -p "$FIGURAS"
java $JAVA_OPTS -cp "$JAR" br.unisinos.edu.graficos.PlotGenerator "$SAIDA" "$FIGURAS"

# 9. Tabela de compromisso (Etapa 12): lê o mesmo CSV
echo "--- Gerando tabela de compromisso ---"
java $JAVA_OPTS -cp "$JAR" br.unisinos.edu.graficos.TradeoffTable "$SAIDA" "$TABELA"

echo "Fim:      $(date '+%Y-%m-%d %H:%M:%S')"
