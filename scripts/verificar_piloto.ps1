# scripts/verificar_piloto.ps1
# Uso: .\scripts\verificar_piloto.ps1 resultados\piloto\piloto_f2.csv
# Arquivo somente ASCII, para não quebrar acentos no Windows PowerShell.

param(
    [Parameter(Mandatory = $true)][string]$Arquivo
)

$script:falhas = 0

function Checar([bool]$ok, [string]$msg) {
    if ($ok) {
        Write-Host "[OK]     $msg" -ForegroundColor Green
    } else {
        Write-Host "[FALHOU] $msg" -ForegroundColor Red
        $script:falhas++
    }
}

function Grupo([string]$esq, [string]$canal, [string]$cod, [string]$ebn0) {
    return @($dados | Where-Object {
        $_.esquema -eq $esq -and $_.canal -eq $canal -and $_.codigo -eq $cod -and $_.ebn0_db -eq $ebn0
    })
}

function MediaBer([string]$esq, [string]$canal, [string]$cod, [string]$ebn0) {
    $g = Grupo $esq $canal $cod $ebn0
    if ($g.Count -eq 0) { return [double]::NaN }
    return ($g | ForEach-Object { [double]$_.ber } | Measure-Object -Average).Average
}

function SomaErros([string]$esq, [string]$canal, [string]$cod, [string]$ebn0) {
    $g = Grupo $esq $canal $cod $ebn0
    if ($g.Count -eq 0) { return [double]::NaN }
    return ($g | ForEach-Object { [double]$_.bits_errados } | Measure-Object -Sum).Sum
}

$ESQUEMAS = 'BPSK', 'QPSK', 'QAM16', 'AMI', 'MANCHESTER'
$EBN0     = '-2.0', '0.0', '2.0', '4.0', '6.0', '8.0', '10.0', '12.0'

# 1. Formato e contagem
$linhas = Get-Content $Arquivo
$cabecalho = 'esquema,tipo,canal,codigo,ebn0_db,semente,bits_mensagem,bits_transmitidos,bits_errados,ber,taxa_codigo,eficiencia_bits_s_hz,sigma,p,r,fator'
Checar ($linhas[0] -eq $cabecalho) 'cabecalho igual ao esperado'
Checar ($linhas.Count -eq 321) "320 linhas de dados (encontradas $($linhas.Count - 1))"

$dados = @(Import-Csv $Arquivo)
Checar (@($dados | Group-Object esquema, canal, codigo, ebn0_db, semente).Count -eq 320) `
    'as 320 combinacoes aparecem, sem repeticao'

# 2. Consistencia interna
$berErrada = @($dados | Where-Object {
    [math]::Abs([double]$_.ber - [double]$_.bits_errados / [double]$_.bits_mensagem) -gt 1e-9
}).Count
Checar ($berErrada -eq 0) 'BER = bits_errados / bits_mensagem em todas as linhas'
Checar (@($dados | Where-Object { [int]$_.bits_mensagem -ne 100000 }).Count -eq 0) `
    'bits_mensagem = 100000 em todas as linhas'

$camposErrados = @($dados | Where-Object {
    if ($_.canal -eq 'AWGN') { $_.p -ne '' -or $_.r -ne '' -or $_.fator -ne '' }
    else                     { $_.p -eq '' -or $_.r -eq '' -or $_.fator -eq '' }
}).Count
Checar ($camposErrados -eq 0) 'p, r e fator vazios no AWGN e preenchidos nas RAJADAS'

# 3. Sigma das rajadas = sigma nominal do AWGN no mesmo ponto
$sigmaAwgn = @{}
foreach ($x in @($dados | Where-Object { $_.canal -eq 'AWGN' })) {
    $sigmaAwgn["$($x.esquema)|$($x.codigo)|$($x.ebn0_db)|$($x.semente)"] = [double]$x.sigma
}
$sigmaDivergente = 0
foreach ($x in @($dados | Where-Object { $_.canal -eq 'RAJADAS' })) {
    $chave = "$($x.esquema)|$($x.codigo)|$($x.ebn0_db)|$($x.semente)"
    if ([math]::Abs($sigmaAwgn[$chave] - [double]$x.sigma) -gt 1e-12) { $sigmaDivergente++ }
}
Checar ($sigmaDivergente -eq 0) 'sigma das RAJADAS igual ao do AWGN no mesmo Eb/N0'

# 4. Controle teorico: BPSK sem codigo no AWGN, Q(sqrt(2 Eb/N0)), tolerancia de 15%
$teoria = @{ '0.0' = 0.0787; '2.0' = 0.0374; '4.0' = 0.0125; '6.0' = 0.00239 }
foreach ($e in '0.0', '2.0', '4.0', '6.0') {
    $m = MediaBer 'BPSK' 'AWGN' 'NENHUM' $e
    $erro = [math]::Abs($m - $teoria[$e]) / $teoria[$e]
    Checar ($erro -le 0.15) ("BPSK AWGN sem codigo, Eb/N0 $e dB: BER {0:E2}, teorica {1:E2}" -f $m, $teoria[$e])
}

# 5. Hamming: comparacao por contagem de erros em 10 e 12 dB.
# A teoria preve que o codigo nao piore BPSK, QPSK, 16-QAM e Manchester nesses pontos.
# So falha se a piora passar de 3 desvios de Poisson, o que evita reprovar diferencas de 1 ou 2 erros.
# AMI fica fora deste criterio: seu cruzamento esperado e perto de 11 dB (ver secao 6).
$esquemasHamming = 'BPSK', 'QPSK', 'QAM16', 'MANCHESTER'
$violHamming = @()
foreach ($esq in $esquemasHamming) {
    foreach ($e in '10.0', '12.0') {
        $sem = SomaErros $esq 'AWGN' 'NENHUM' $e
        $com = SomaErros $esq 'AWGN' 'HAMMING74' $e
        if (($com - $sem) -gt (3 * [math]::Sqrt($sem + $com))) {
            $violHamming += ("{0} {1} dB: com codigo {2} erros, sem {3}" -f $esq, $e, $com, $sem)
        }
    }
}
Checar ($violHamming.Count -eq 0) 'Hamming nao e significativamente pior em 10 e 12 dB (BPSK, QPSK, QAM16, Manchester)'
$violHamming | ForEach-Object { Write-Host "         $_" -ForegroundColor Yellow }

# 6. Informativo (nao falha): erros absolutos, sem codigo x Hamming, no AWGN
Write-Host ''
Write-Host 'Erros absolutos (AWGN): sem codigo x Hamming74.'
Write-Host 'Esperado: AMI pior com Hamming ate ~11 dB; BPSK, QPSK e Manchester cruzam em ~6 dB; 16-QAM perto de 9 dB.'
$tabErros = foreach ($esq in 'BPSK', 'QPSK', 'QAM16', 'AMI', 'MANCHESTER') {
    foreach ($e in $EBN0) {
        [pscustomobject]@{
            esquema = $esq
            ebn0_db = $e
            sem     = (SomaErros $esq 'AWGN' 'NENHUM' $e)
            com     = (SomaErros $esq 'AWGN' 'HAMMING74' $e)
        }
    }
}
$tabErros | Format-Table -AutoSize

# 7. Informativo (nao falha): BER media por esquema, AWGN x RAJADAS
Write-Host ''
Write-Host 'BER media (sem codigo), para comparar os fatores:'
$tabela = foreach ($e in $EBN0) {
    foreach ($esq in 'BPSK', 'AMI') {
        $a = MediaBer $esq 'AWGN' 'NENHUM' $e
        $r = MediaBer $esq 'RAJADAS' 'NENHUM' $e
        [pscustomobject]@{
            esquema = $esq
            ebn0_db = $e
            AWGN    = ('{0:E2}' -f $a)
            RAJADAS = ('{0:E2}' -f $r)
            razao   = $(if ($a -gt 0) { '{0:N1}' -f ($r / $a) } else { '-' })
        }
    }
}
$tabela | Format-Table -AutoSize

# 8. Rajadas nao podem ser melhores que o AWGN onde a BER do AWGN e significativa
$rajadasMelhores = @()
foreach ($esq in $ESQUEMAS) {
    foreach ($e in $EBN0) {
        $a = MediaBer $esq 'AWGN' 'NENHUM' $e
        $r = MediaBer $esq 'RAJADAS' 'NENHUM' $e
        if ($a -gt 1e-3 -and $r -lt $a) {
            $rajadasMelhores += ("{0} {1} dB: rajadas {2:E2} < AWGN {3:E2}" -f $esq, $e, $r, $a)
        }
    }
}
Checar ($rajadasMelhores.Count -eq 0) 'rajadas nao melhores que o AWGN (BER do AWGN > 1e-3)'
$rajadasMelhores | ForEach-Object { Write-Host "         $_" -ForegroundColor Yellow }

Write-Host ''
if ($script:falhas -eq 0) {
    Write-Host 'Verificacoes tecnicas passaram.' -ForegroundColor Green
} else {
    Write-Host "$($script:falhas) verificacao(oes) falharam." -ForegroundColor Red
}