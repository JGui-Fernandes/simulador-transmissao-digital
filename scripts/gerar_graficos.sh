#!/usr/bin/env bash
# scripts/gerar_graficos.sh
# Uso: ./scripts/gerar_graficos.sh <arquivo.csv> [diretorio_saida]

set -euo pipefail

# 1. Validacao de argumentos
if [ $# -lt 1 ]; then
    echo "Erro: Parametro obrigatorio ausente." >&2
    echo "Uso: $0 <caminho_do_csv> [diretorio_saida]" >&2
    exit 1
fi

CSV_INPUT="$1"
SAIDA="${2:-docs/figuras}"

# 2. Localizacao do JAR
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR="${SCRIPT_DIR}/../target/gb.jar"

if [ ! -f "$JAR" ]; then
    echo "Erro: target/gb.jar nao encontrado. Rode 'mvn clean package' na raiz do projeto." >&2
    exit 1
fi

if [ ! -f "$CSV_INPUT" ]; then
    echo "Erro: Arquivo CSV '$CSV_INPUT' nao encontrado." >&2
    exit 1
fi

# 3. Resolver caminho absoluto do CSV
CSV_ABSOLUTO="$(cd "$(dirname "$CSV_INPUT")" && pwd)/$(basename "$CSV_INPUT")"

# 4. Criar diretorio de saida se nao existir
mkdir -p "$SAIDA"

# 5. Execucao do gerador Java
java -cp "$JAR" br.unisinos.edu.graficos.PlotGenerator "$CSV_ABSOLUTO" "$SAIDA"

echo "Figuras geradas em $SAIDA"