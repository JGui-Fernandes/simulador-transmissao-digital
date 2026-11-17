param(
    [Parameter(Mandatory = $true)]
    [string] $Csv,

    [string] $Saida = "docs\figuras"
)

$ErrorActionPreference = "Stop"

$jar = Join-Path $PSScriptRoot "..\target\gb.jar"
if (-not (Test-Path $jar)) {
    throw "target\gb.jar nao encontrado. Rode 'mvn clean package' na raiz do projeto."
}

$csvAbsoluto = (Resolve-Path $Csv).Path

java -cp $jar br.unisinos.edu.graficos.PlotGenerator $csvAbsoluto $Saida
if ($LASTEXITCODE -ne 0) {
    throw "PlotGenerator terminou com codigo $LASTEXITCODE"
}

Write-Host "Figuras geradas em $Saida"