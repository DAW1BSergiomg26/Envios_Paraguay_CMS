param(
    [Parameter(Mandatory=$true)]
    [string]$BackupFile
)

if (-not (Test-Path $BackupFile)) {
    Write-Error "File not found: $BackupFile"
    exit 1
}

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

Write-Host "Restoring database '$($env:MYSQL_DATABASE)' from $(Split-Path $BackupFile -Leaf)..."

if ($BackupFile -match '\.zip$') {
    $tempFile = Join-Path $env:TEMP "restore_$(Get-Random).sql"
    Expand-Archive -Path $BackupFile -DestinationPath $env:TEMP -Force
    $extracted = Get-ChildItem -Path $env:TEMP -Filter "*.sql" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $extracted) {
        Write-Error "No .sql file found in archive"
        exit 1
    }
    Get-Content $extracted.FullName | docker exec -i monteastur-mysql mysql -u root -p"$($env:MYSQL_ROOT_PASSWORD)" $env:MYSQL_DATABASE
    Remove-Item $extracted.FullName
} else {
    Get-Content $BackupFile | docker exec -i monteastur-mysql mysql -u root -p"$($env:MYSQL_ROOT_PASSWORD)" $env:MYSQL_DATABASE
}

Write-Host "Restore completed successfully."
