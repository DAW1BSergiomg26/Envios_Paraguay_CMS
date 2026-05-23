param(
    [string]$BackupDir = (Join-Path (Split-Path $PSScriptRoot -Parent) "backup\db")
)

$envFile = Join-Path (Split-Path $PSScriptRoot -Parent) ".env"

if (-not (Test-Path $envFile)) {
    Write-Error ".env not found at $envFile"
    exit 1
}

$envContent = Get-Content $envFile | Where-Object { $_ -match '^[^#]' -and $_ -match '=' }
foreach ($line in $envContent) {
    $kvp = $line -split '=', 2
    Set-Item -Path "env:$($kvp[0])" -Value $kvp[1]
}

if (-not $env:MYSQL_ROOT_PASSWORD -or -not $env:MYSQL_DATABASE) {
    Write-Error "MYSQL_ROOT_PASSWORD or MYSQL_DATABASE not set"
    exit 1
}

New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null

$timestamp = Get-Date -Format "yyyy-MM-dd_HH-mm"
$backupFile = Join-Path $BackupDir "$timestamp.sql"

Write-Host "Backing up database '$($env:MYSQL_DATABASE)'..."
$tempFile = Join-Path $env:TEMP "$timestamp.sql"
docker exec monteastur-mysql mysqldump -u root -p"$($env:MYSQL_ROOT_PASSWORD)" $env:MYSQL_DATABASE > $tempFile

if ($LASTEXITCODE -ne 0) {
    Write-Error "mysqldump failed"
    exit 1
}

Compress-Archive -Path $tempFile -DestinationPath "$backupFile.zip" -Force
Remove-Item $tempFile

Write-Host "Backup created: $backupFile.zip"
