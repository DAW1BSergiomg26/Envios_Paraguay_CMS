param(
    [string]$BackupDir = (Join-Path (Split-Path $PSScriptRoot -Parent) "backup\uploads")
)

New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null

$timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm"
$backupFile = Join-Path $BackupDir "$timestamp.tar.gz"

Write-Host "Backing up uploads from monteastur-app:/app/uploads..."

$tempDir = Join-Path $env:TEMP "uploads-backup-$timestamp"
New-Item -ItemType Directory -Path $tempDir -Force | Out-Null

docker cp "monteastur-app:/app/uploads/." "$tempDir" 2>$null

# Create tar.gz using PowerShell tar alias (Windows 10+)
tar -czf $backupFile -C $tempDir .

Remove-Item -Path $tempDir -Recurse -Force

Write-Host "Backup created: $backupFile"
