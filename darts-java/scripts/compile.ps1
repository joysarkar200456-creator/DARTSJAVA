$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location "$scriptDir\.."
if (!(Test-Path "out")) {
    New-Item -ItemType Directory -Path "out" | Out-Null
}
$sources = Get-ChildItem -Path "src" -Recurse -Include *.java | Select-Object -ExpandProperty FullName
javac -d out $sources
if ($LASTEXITCODE -eq 0) {
    Write-Host "[SUCCESS] Compilation finished clean." -ForegroundColor Green
} else {
    Write-Host "[ERROR] Compilation failed." -ForegroundColor Red
    exit $LASTEXITCODE
}
