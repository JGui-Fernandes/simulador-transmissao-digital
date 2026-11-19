#!/usr/bin/env bash
# scripts/verificar_piloto.sh
# Uso: ./scripts/verificar_piloto.sh resultados/piloto/piloto_f2.csv

set -euo pipefail

if [ $# -lt 1 ]; then
    echo "Uso: $0 <caminho_do_arquivo.csv>" >&2
    exit 1
fi

ARQUIVO="$1"

if [ ! -f "$ARQUIVO" ]; then
    echo "Erro: Arquivo '$ARQUIVO' nao encontrado." >&2
    exit 1
fi

# Executa as validacoes estatisticas e tecnicas em Python nativo (padrao no Linux/macOS)
python3 - "$ARQUIVO" << 'EOF'
import sys
import csv
import math

arquivo = sys.argv[1]
falhas = 0

GREEN = "\033[32m"
RED = "\033[31m"
YELLOW = "\033[33m"
RESET = "\033[0m"

def checar(ok, msg):
    global falhas
    if ok:
        print(f"{GREEN}[OK]{RESET}     {msg}")
    else:
        print(f"{RED}[FALHOU]{RESET} {msg}")
        falhas += 1

ESQUEMAS = ['BPSK', 'QPSK', 'QAM16', 'AMI', 'MANCHESTER']
EBN0 = ['-2.0', '0.0', '2.0', '4.0', '6.0', '8.0', '10.0', '12.0']

# 1. Formato e contagem
with open(arquivo, 'r', encoding='utf-8') as f:
    linhas = [l.strip() for l in f if l.strip()]

cabecalho_esperado = 'esquema,tipo,canal,codigo,ebn0_db,semente,bits_mensagem,bits_transmitidos,bits_errados,ber,taxa_codigo,eficiencia_bits_s_hz,sigma,p,r,fator'
checar(linhas[0] == cabecalho_esperado, 'cabecalho igual ao esperado')
checar(len(linhas) == 321, f'320 linhas de dados (encontradas {len(linhas) - 1})')

with open(arquivo, 'r', encoding='utf-8') as f:
    reader = csv.DictReader(f)
    dados = list(reader)

combinacoes = set((d['esquema'], d['canal'], d['codigo'], d['ebn0_db'], d['semente']) for d in dados)
checar(len(combinacoes) == 320, 'as 320 combinacoes aparecem, sem repeticao')

# Funcoes auxiliares
def grupo(esq, canal, cod, ebn0):
    return [d for d in dados if d['esquema'] == esq and d['canal'] == canal and d['codigo'] == cod and d['ebn0_db'] == ebn0]

def media_ber(esq, canal, cod, ebn0):
    g = grupo(esq, canal, cod, ebn0)
    if not g: return float('nan')
    return sum(float(x['ber']) for x in g) / len(g)

def soma_erros(esq, canal, cod, ebn0):
    g = grupo(esq, canal, cod, ebn0)
    if not g: return float('nan')
    return sum(float(x['bits_errados']) for x in g)

# 2. Consistencia interna
ber_errada = sum(1 for d in dados if abs(float(d['ber']) - float(d['bits_errados']) / float(d['bits_mensagem'])) > 1e-9)
checar(ber_errada == 0, 'BER = bits_errados / bits_mensagem em todas as linhas')

msg_len_errada = sum(1 for d in dados if int(d['bits_mensagem']) != 100000)
checar(msg_len_errada == 0, 'bits_mensagem = 100000 em todas as linhas')

def campos_invalidos(d):
    if d['canal'] == 'AWGN':
        return bool(d.get('p') or d.get('r') or d.get('fator'))
    else:
        return not bool(d.get('p') and d.get('r') and d.get('fator'))

campos_errados = sum(1 for d in dados if campos_invalidos(d))
checar(campos_errados == 0, 'p, r e fator vazios no AWGN e preenchidos nas RAJADAS')

# 3. Sigma das rajadas = sigma nominal do AWGN
sigma_awgn = {f"{d['esquema']}|{d['codigo']}|{d['ebn0_db']}|{d['semente']}": float(d['sigma']) for d in dados if d['canal'] == 'AWGN'}
sigma_div = sum(1 for d in dados if d['canal'] == 'RAJADAS' and abs(sigma_awgn.get(f"{d['esquema']}|{d['codigo']}|{d['ebn0_db']}|{d['semente']}", 0) - float(d['sigma'])) > 1e-12)
checar(sigma_div == 0, 'sigma das RAJADAS igual ao do AWGN no mesmo Eb/N0')

# 4. Controle teorico: BPSK sem codigo no AWGN
teoria = {'0.0': 0.0787, '2.0': 0.0374, '4.0': 0.0125, '6.0': 0.00239}
for e in ['0.0', '2.0', '4.0', '6.0']:
    m = media_ber('BPSK', 'AWGN', 'NENHUM', e)
    erro = abs(m - teoria[e]) / teoria[e]
    checar(erro <= 0.15, f"BPSK AWGN sem codigo, Eb/N0 {e} dB: BER {m:.2E}, teorica {teoria[e]:.2E}")

# 5. Hamming: Poisson em 10 e 12 dB
esquemas_hamming = ['BPSK', 'QPSK', 'QAM16', 'MANCHESTER']
viol_hamming = []
for esq in esquemas_hamming:
    for e in ['10.0', '12.0']:
        sem = soma_erros(esq, 'AWGN', 'NENHUM', e)
        com = soma_erros(esq, 'AWGN', 'HAMMING74', e)
        if (com - sem) > (3 * math.sqrt(sem + com)):
            viol_hamming.append(f"{esq} {e} dB: com codigo {int(com)} erros, sem {int(sem)}")

checar(len(viol_hamming) == 0, 'Hamming nao e significativamente pior em 10 e 12 dB (BPSK, QPSK, QAM16, Manchester)')
for v in viol_hamming:
    print(f"{YELLOW}         {v}{RESET}")

# 6. Informativo: Erros absolutos
print("\nErros absolutos (AWGN): sem codigo x Hamming74.")
print("Esperado: AMI pior com Hamming ate ~11 dB; BPSK, QPSK e Manchester cruzam em ~6 dB; 16-QAM perto de 9 dB.")
print(f"{'esquema':<12} {'ebn0_db':<8} {'sem':<10} {'com':<10}")
print("-" * 42)
for esq in ['BPSK', 'QPSK', 'QAM16', 'AMI', 'MANCHESTER']:
    for e in EBN0:
        sem = int(soma_erros(esq, 'AWGN', 'NENHUM', e))
        com = int(soma_erros(esq, 'AWGN', 'HAMMING74', e))
        print(f"{esq:<12} {e:<8} {sem:<10} {com:<10}")

# 7. Informativo: BER media
print("\nBER media (sem codigo), para comparar os fatores:")
print(f"{'esquema':<10} {'ebn0_db':<8} {'AWGN':<12} {'RAJADAS':<12} {'razao':<8}")
print("-" * 52)
for e in EBN0:
    for esq in ['BPSK', 'AMI']:
        a = media_ber(esq, 'AWGN', 'NENHUM', e)
        r = media_ber(esq, 'RAJADAS', 'NENHUM', e)
        razao = f"{r/a:.1f}" if a > 0 else "-"
        print(f"{esq:<10} {e:<8} {a:<12.2E} {r:<12.2E} {razao:<8}")

# 8. Rajadas vs AWGN
rajadas_melhores = []
for esq in ESQUEMAS:
    for e in EBN0:
        a = media_ber(esq, 'AWGN', 'NENHUM', e)
        r = media_ber(esq, 'RAJADAS', 'NENHUM', e)
        if a > 1e-3 and r < a:
            rajadas_melhores.append(f"{esq} {e} dB: rajadas {r:.2E} < AWGN {a:.2E}")

checar(len(rajadas_melhores) == 0, 'rajadas nao melhores que o AWGN (BER do AWGN > 1e-3)')
for rm in rajadas_melhores:
    print(f"{YELLOW}         {rm}{RESET}")

print("")
if falhas == 0:
    print(f"{GREEN}Verificacoes tecnicas passaram.{RESET}")
    sys.exit(0)
else:
    print(f"{RED}{falhas} verificacao(oes) falharam.{RESET}")
    sys.exit(1)
EOF