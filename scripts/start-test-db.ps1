param([string]$MySqlBin = 'C:\Program Files\MySQL\MySQL Server 8.0\bin', [int]$Port = 3307)
$ErrorActionPreference = 'Stop'
if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) {
    throw "Port $Port is already in use. This script will not access an existing server."
}
$mysqlServer = Join-Path $MySqlBin 'mysqld.exe'
if (!(Test-Path -LiteralPath $mysqlServer)) { throw 'Set -MySqlBin to your installed MySQL 8 bin directory.' }
$testDirectory = Join-Path ([IO.Path]::GetTempPath()) ('smartcrm-test-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $testDirectory | Out-Null
& $mysqlServer --no-defaults --initialize-insecure "--datadir=$testDirectory" 2> ($testDirectory + '-initialize.log')
if ($LASTEXITCODE -ne 0) { throw "MySQL initialization failed. See $testDirectory" }
$serverProcess = Start-Process -FilePath $mysqlServer -ArgumentList @('--no-defaults', "--datadir=`"$testDirectory`"", "--port=$Port", '--bind-address=127.0.0.1', '--mysqlx=OFF', '--console') -PassThru -WindowStyle Hidden -RedirectStandardOutput (Join-Path $testDirectory 'server.log') -RedirectStandardError (Join-Path $testDirectory 'server-error.log')
try {
    $ready = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        & (Join-Path $MySqlBin 'mysqladmin.exe') --no-defaults --host=127.0.0.1 "--port=$Port" --user=root ping 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) { $ready = $true; break }
        Start-Sleep -Milliseconds 500
    }
    if (!$ready) { throw "MySQL startup failed. See $testDirectory" }
    & (Join-Path $MySqlBin 'mysql.exe') --no-defaults --host=127.0.0.1 "--port=$Port" --user=root --execute='CREATE DATABASE smartcrm_l8_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;'
    if ($LASTEXITCODE -ne 0) { throw 'Cannot create isolated test schema' }
    $listeningProcess = Get-NetTCPConnection -LocalPort $Port -State Listen | Select-Object -First 1 -ExpandProperty OwningProcess
    [pscustomobject]@{ ProcessId = $listeningProcess; MonitorProcessId = $serverProcess.Id; Port = $Port; DataDirectory = $testDirectory }
} catch {
    Stop-Process -Id $serverProcess.Id -ErrorAction SilentlyContinue
    throw
}
