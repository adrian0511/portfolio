# Genera los PDF del CV a partir del HTML de esta carpeta.
#
#   pwsh cv/render.ps1
#
# Chrome headless en vez de wkhtmltopdf (que fue quien generó los PDF viejos):
# wkhtmltopdf 0.12.x va sobre un WebKit de 2012 y no soporta flexbox, y además
# ya no se mantiene. Chrome está instalado en cualquier máquina donde se toque
# este repo.
#
# --no-pdf-header-footer es obligatorio: sin él Chrome estampa la URL y la fecha
# en cada página, y eso acaba dentro del texto que lee el ATS.

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

  # Borrar antes de llamar a Chrome: el Test-Path de abajo pasaba siempre que
  # hubiera un PDF de una tirada anterior, asi que si Chrome fallaba (cambio de
  # version, fichero abierto en un visor, flag retirado) el script decia "Listo"
  # y se publicaba el CV viejo creyendo haberlo regenerado.
  Remove-Item $out -ErrorAction Ignore

  # Sin 2>$null: Chrome escribe en stderr cosas que hay que leer cuando no sale
  # el PDF, y tragarselas era la otra mitad del problema de arriba.
  & $chrome --headless --disable-gpu --no-pdf-header-footer `
    "--print-to-pdf=$out" "file:///$($src -replace '\\','/')"

  # El headless nuevo se desentiende del proceso que escribe el PDF, asi que al
  # volver de la llamada el fichero puede no estar todavia: hay que esperarlo, y
  # esperar a que deje de crecer. Sin esto el Test-Path de abajo fallaba aunque
  # Chrome acabara bien, que es lo que escondia el PDF viejo sin borrar.
  $limite = (Get-Date).AddSeconds(30)
  $anterior = -1
  do {
    Start-Sleep -Milliseconds 400
    $actual = if (Test-Path $out) { (Get-Item $out).Length } else { -1 }
    $estable = $actual -gt 0 -and $actual -eq $anterior
    $anterior = $actual
  } until ($estable -or (Get-Date) -gt $limite)

  if (-not $estable) { throw "Chrome no genero $out" }
  Write-Host ("{0} -> {1}" -f $lang, $out)
}

Write-Host "Listo. Revisa que cada PDF siga cabiendo en UNA página."
