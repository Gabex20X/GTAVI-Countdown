$ErrorActionPreference = "Stop"

$jdk = "C:\Program Files\Java\jdk-21\bin"
$root = $PSScriptRoot
$srcDir = Join-Path $root "src"
$buildDir = Join-Path $root "build"
$classesDir = Join-Path $buildDir "classes"
$distDir = Join-Path $root "dist"

function Remove-DirWithRetry($path) {
    for ($i = 0; $i -lt 5; $i++) {
        if (-not (Test-Path $path)) { return }
        try {
            Remove-Item $path -Recurse -Force -ErrorAction Stop
            return
        } catch {
            Start-Sleep -Milliseconds 500
        }
    }
    Remove-Item $path -Recurse -Force -ErrorAction SilentlyContinue
}

Remove-DirWithRetry $buildDir
Remove-DirWithRetry $distDir
New-Item -ItemType Directory -Force -Path $classesDir | Out-Null

$sources = Get-ChildItem $srcDir -Filter *.java -Recurse | ForEach-Object { $_.FullName }
& "$jdk\javac.exe" -d $classesDir $sources
if ($LASTEXITCODE -ne 0) { throw "javac falhou (codigo $LASTEXITCODE)" }

$fontsDir = Join-Path $srcDir "fonts"
if (Test-Path $fontsDir) {
    Copy-Item $fontsDir -Destination $classesDir -Recurse -Force
}

$iconDir = Join-Path $srcDir "icon"
if (Test-Path $iconDir) {
    Copy-Item $iconDir -Destination $classesDir -Recurse -Force
}

& "$jdk\jar.exe" --create --file "$buildDir\GTA6Countdown.jar" --main-class CountdownApp -C $classesDir .
if ($LASTEXITCODE -ne 0) { throw "jar falhou (codigo $LASTEXITCODE)" }

& "$jdk\jpackage.exe" `
    --type app-image `
    --input $buildDir `
    --name "GTA6Countdown" `
    --main-jar "GTA6Countdown.jar" `
    --main-class CountdownApp `
    --icon "$srcDir\icon\app.ico" `
    --dest $distDir
if ($LASTEXITCODE -ne 0) { throw "jpackage falhou (codigo $LASTEXITCODE)" }

Write-Output "Build concluido: $distDir\GTA6Countdown\GTA6Countdown.exe"
