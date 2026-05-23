param(
    [Parameter(Mandatory=$true)]
    [string]$BackupFile
)

if (-not (Test-Path $BackupFile)) {
    Write-Error "File not found: $BackupFile"
    exit 1
}

$backupDir = Split-Path $BackupFile -Parent
$timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
$preRestoreFile = Join-Path $backupDir "_pre-restore-$timestamp.tar.gz"

Write-Host "Creating backup of current uploads before restore..."
$tempDir = Join-Path $env:TEMP "pre-restore-$timestamp"
New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
docker cp "monteastur-app:/app/uploads/." "$tempDir" 2>$null
tar -czf $preRestoreFile -C $tempDir .
Remove-Item -Path $tempDir -Recurse -Force
Write-Host "Current uploads saved to: $preRestoreFile"

Write-Host "Restoring uploads from $(Split-Path $BackupFile -Leaf)..."
$restoreDir = Join-Path $env:TEMP "restore-uploads-$timestamp"
New-Item -ItemType Directory -Path $restoreDir -Force | Out-Null
tar -xzf $BackupFile -C $restoreDir
Get-ChildItem -Path $restoreDir -Recurse | ForEach-Object {
    $relative = $_.FullName.Substring($restoreDir.Length + 1)
    if ($relative) {
        $containerPath = "/app/uploads/$($relative -replace '\\', '/')"
        if (-not $_.PSIsContainer) {
            $parentPath = Split-Path $containerPath -Parent
            docker exec monteastur-app mkdir -p "$parentPath" 2>$null
            docker cp $_.FullName "monteastur-app:$containerPath"
        }
    }
}
Remove-Item -Path $restoreDir -Recurse -Force

Write-Host "Restore completed successfully."
