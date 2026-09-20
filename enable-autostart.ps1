$exePath = Join-Path $PSScriptRoot "dist\GTA6Countdown\GTA6Countdown.exe"
if (-not (Test-Path $exePath)) {
    Write-Output "Nao encontrei $exePath - rode build.ps1 primeiro."
    exit 1
}

$startupDir = [Environment]::GetFolderPath("Startup")
$shortcutPath = Join-Path $startupDir "GTA6Countdown.lnk"

$ws = New-Object -ComObject WScript.Shell
$shortcut = $ws.CreateShortcut($shortcutPath)
$shortcut.TargetPath = $exePath
$shortcut.WorkingDirectory = Split-Path $exePath
$shortcut.Save()

Write-Output "Atalho de auto-start criado em: $shortcutPath"
