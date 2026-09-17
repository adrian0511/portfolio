# Genera los PDF del CV a partir del HTML de esta carpeta.
#
#   pwsh cv/render.ps1
#
# Chrome headless en vez de wkhtmltopdf (que fue quien generó los PDF viejos):
# wkhtmltopdf 0.12.x va sobre un WebKit de 2012 y no soporta flexbox, y además
# ya no se mantiene. Chrome está instalado en cualquier máquina donde se toque
# este repo.
#
# -NoHeaderFooter es obligatorio: sin él Chrome estampa la URL y la fecha en
# cada página, y eso acaba dentro del texto que lee el ATS.

$ErrorActionPreference = 'Stop'

$chrome = @(
  "$env:ProgramFiles\Google\Chrome\Application\chrome.exe",
  "${env:ProgramFiles(x86)}\Google\Chrome\Application\chrome.exe",
  "$env:LOCALAPPDATA\Google\Chrome\Application\chrome.exe"
) | Where-Object { Test-Path $_ } | Select-Object -First 1

if (-not $chrome) { throw "No encuentro chrome.exe; instala Chrome o ajusta la ruta." }

$cvDir = $PSScriptRoot
$outDir = Join-Path (Split-Path $cvDir -Parent) 'frontend\public\docs'

foreach ($lang in 'ES', 'EN') {
  $src = Join-Path $cvDir ("cv-{0}.html" -f $lang.ToLower())
  $out = Join-Path $outDir ("CV_Adrian_Garces_{0}.pdf" -f $lang)

  & $chrome --headless --disable-gpu --no-pdf-header-footer `
    "--print-to-pdf=$out" "file:///$($src -replace '\\','/')" 2>$null

  if (-not (Test-Path $out)) { throw "No se generó $out" }
  Write-Host ("{0} -> {1}" -f $lang, $out)
}

Write-Host "Listo. Revisa que cada PDF siga cabiendo en UNA página."
