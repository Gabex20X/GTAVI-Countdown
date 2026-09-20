$startupDir = [Environment]::GetFolderPath("Startup")
$shortcutPath = Join-Path $startupDir "GTA6Countdown.lnk"

if (Test-Path $shortcutPath) {
    Remove-Item $shortcutPath -Force
    Write-Output "Atalho removido: $shortcutPath"
} else {
    Write-Output "Nenhum atalho encontrado em $shortcutPath"
}
