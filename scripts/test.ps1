param([string]$MySqlBin = 'C:\Program Files\MySQL\MySQL Server 8.0\bin')
$ErrorActionPreference = 'Stop'
$database = & "$PSScriptRoot/start-test-db.ps1" -MySqlBin $MySqlBin
try {
    Push-Location "$PSScriptRoot/../backend"
    try {
        # Force this newly owned database even if the caller has TEST_DB_* configured.
        & ./mvnw.cmd test "-Dspring.datasource.url=jdbc:mysql://127.0.0.1:$($database.Port)/smartcrm_l8_test?connectionTimeZone=Asia/Ho_Chi_Minh" '-Dspring.datasource.username=root' '-Dspring.datasource.password='
        if ($LASTEXITCODE -ne 0) { throw 'Backend tests failed' }
    } finally { Pop-Location }
} finally {
    $runningServer = Get-CimInstance Win32_Process -Filter "ProcessId=$($database.ProcessId)"
    if ($runningServer -and $runningServer.CommandLine.Contains($database.DataDirectory)) {
        & (Join-Path $MySqlBin 'mysqladmin.exe') --no-defaults --host=127.0.0.1 "--port=$($database.Port)" --user=root shutdown
        if ($LASTEXITCODE -ne 0) { throw 'Could not shut down the disposable test server' }
    }
    Write-Host "Test data and logs retained at $($database.DataDirectory). Existing MySQL untouched."
}
