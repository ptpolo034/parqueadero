# Genera el instalador .exe de Parqueadero (con Java incluido, no requiere instalar nada aparte).
#
# Uso: abre PowerShell en la carpeta del proyecto y corre:
#   .\generar-instalador.ps1
#
# El instalador queda en target\instalador\Parqueadero-<version>.exe

$ErrorActionPreference = "Stop"

# Sube este numero cada vez que generes una version nueva para distribuir.
$version = "1.0.0"

# NO CAMBIAR NUNCA este identificador: es lo que le permite a Windows detectar que un
# instalador nuevo es una ACTUALIZACION del mismo programa (reemplaza en el mismo lugar,
# sin crear una instalacion aparte ni pedir desinstalar la anterior primero).
$upgradeUuid = "83271778-5555-4c20-a22f-eeb4ad9dad5c"

$mvn = "C:\Program Files\Apache\Maven\apache-maven-3.9.16\bin\mvn.cmd"
$jdkBin = "C:\Program Files\Java\jdk-21.0.12.1\bin"

Write-Host "== Compilando el proyecto ==" -ForegroundColor Cyan
& $mvn clean package
if ($LASTEXITCODE -ne 0) { throw "Fallo la compilacion con Maven" }

Write-Host "== Preparando carpeta de entrada para jpackage ==" -ForegroundColor Cyan
Remove-Item -Recurse -Force "target\jpackage-input" -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path "target\jpackage-input" | Out-Null
Copy-Item "target\parqueadero.jar" "target\jpackage-input\parqueadero.jar"

Write-Host "== Generando instalador (esto puede tardar un par de minutos) ==" -ForegroundColor Cyan
Remove-Item -Recurse -Force "target\instalador" -ErrorAction SilentlyContinue
& "$jdkBin\jpackage.exe" `
    --type exe `
    --name "Parqueadero" `
    --input "target\jpackage-input" `
    --main-jar parqueadero.jar `
    --main-class com.parqueadero.Main `
    --icon "recursos\icono.ico" `
    --app-version $version `
    --vendor "Henry Polo" `
    --description "Sistema de Parqueadero y Lavado" `
    --win-shortcut `
    --win-menu `
    --win-dir-chooser `
    --win-per-user-install `
    --win-upgrade-uuid $upgradeUuid `
    --dest "target\instalador"

if ($LASTEXITCODE -ne 0) { throw "Fallo jpackage" }

Write-Host ""
Write-Host "Listo. Instalador generado en target\instalador\" -ForegroundColor Green
Get-ChildItem "target\instalador" | Format-Table Name, Length
