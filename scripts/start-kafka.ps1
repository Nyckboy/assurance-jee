param(
    # Kafka installation folder (unpacked kafka_2.13-<version>.tgz). Keep the path short:
    # Kafka's Windows scripts fail with "The input line is too long" in deep folders.
    [string]$KafkaHome = $(if ($env:KAFKA_HOME) { $env:KAFKA_HOME } else { Join-Path $HOME 'tools\kafka' }),
    # Where the broker stores its data. Delete this folder to start again from an empty broker.
    [string]$DataDir = $(Join-Path $HOME 'tools\kafka-data')
)

# Starts a single-node Kafka broker (KRaft mode, no ZooKeeper) on localhost:9092, in this window.
# Press Ctrl+C to stop it. See docs/LOCAL_DEVELOPMENT.md.

$ErrorActionPreference = 'Stop'

$startScript = Join-Path $KafkaHome 'bin\windows\kafka-server-start.bat'
$storageScript = Join-Path $KafkaHome 'bin\windows\kafka-storage.bat'
if (-not (Test-Path -LiteralPath $startScript)) {
    throw "Kafka was not found in '$KafkaHome'. Unpack kafka_2.13-<version>.tgz there, or pass -KafkaHome <folder> (or set KAFKA_HOME)."
}

if (Get-NetTCPConnection -LocalPort 9092 -State Listen -ErrorAction SilentlyContinue) {
    throw 'Port 9092 is already in use: a Kafka broker is probably already running.'
}

New-Item -ItemType Directory -Path $DataDir -Force | Out-Null
$logDir = Join-Path $DataDir 'logs'
$configFile = Join-Path $DataDir 'server.properties'

# Our own copy of the default single-node config, pointing at our data folder.
# Kafka's installation files are never modified.
$logDirForKafka = $logDir -replace '\\', '/'
(Get-Content -LiteralPath (Join-Path $KafkaHome 'config\server.properties')) `
    -replace '^log\.dirs=.*', "log.dirs=$logDirForKafka" |
    Set-Content -LiteralPath $configFile -Encoding ascii

# Kafka's .bat tools print harmless log4j warnings on stderr, which Windows PowerShell would treat as
# terminating errors. From here on, the tools' exit codes decide success.
$ErrorActionPreference = 'Continue'

# First start only: format the storage with a new cluster id.
if (-not (Test-Path -LiteralPath (Join-Path $logDir 'meta.properties'))) {
    $clusterId = & $storageScript random-uuid 2>$null |
        Where-Object { $_ -match '^[A-Za-z0-9_-]{22}$' } |
        Select-Object -Last 1
    if (-not $clusterId) {
        throw 'Could not generate a Kafka cluster id (kafka-storage.bat random-uuid).'
    }
    Write-Host "Formatting Kafka storage in $logDir (cluster id $clusterId)."
    & $storageScript format --standalone -t $clusterId -c $configFile 2>&1 |
        Where-Object { $_ -notmatch 'Reconfiguration failed' } | ForEach-Object { "$_" }
    if ($LASTEXITCODE -ne 0) {
        throw 'Formatting the Kafka storage failed.'
    }
}

# kafka-server-start.bat only picks a heap size when KAFKA_HEAP_OPTS is unset, and it does so with
# wmic, which recent Windows 11 versions no longer include. Setting it skips that step.
if (-not $env:KAFKA_HEAP_OPTS) {
    $env:KAFKA_HEAP_OPTS = '-Xmx512M -Xms512M'
}

Write-Host "Starting Kafka on localhost:9092 (data in $DataDir). Press Ctrl+C to stop it."
& $startScript $configFile
exit $LASTEXITCODE
