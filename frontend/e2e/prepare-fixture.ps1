param(
    [string]$MySqlBin = 'C:\Program Files\MySQL\MySQL Server 8.0\bin',
    [int]$TestPort = 3307,
    [string]$TestDatabase = 'smartcrm_l8_test',
    [string]$TestUser = 'root',
    [string]$ApiBase = 'http://127.0.0.1:8080'
)
$ErrorActionPreference = 'Stop'
if ($TestPort -eq 3306 -or $TestDatabase -notmatch '^[A-Za-z0-9_]+_test$') {
    throw 'E2E fixture requires a disposable schema ending in _test and a dedicated MySQL port other than 3306.'
}
if (!$env:E2E_SERVICE_KEY) { throw 'Set E2E_SERVICE_KEY for the test backend before preparing a fixture.' }
try {
    $null = Invoke-WebRequest -UseBasicParsing -Uri "$ApiBase/api/auth/me" -TimeoutSec 5
} catch {
    if (!$_.Exception.Response -or [int]$_.Exception.Response.StatusCode -ne 401) {
        throw 'Test backend is not ready at ApiBase. Start it against the disposable MySQL database before inserting a fixture.'
    }
}
$fixtureId = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$fixtureCode = "E2E-$fixtureId"
$sql = "INSERT INTO ticket(ticket_id,ticket_code,customer_id,center_id,technician_id,device_id,status,closed_at) VALUES($fixtureId,'$fixtureCode',201,101,301,501,'DA_DONG',CURRENT_TIMESTAMP);"
$previousPassword = $env:MYSQL_PWD
try {
    $env:MYSQL_PWD = $env:TEST_DB_PASSWORD
    & (Join-Path $MySqlBin 'mysql.exe') --no-defaults --host=127.0.0.1 "--port=$TestPort" "--user=$TestUser" "--database=$TestDatabase" "--execute=$sql"
    if ($LASTEXITCODE -ne 0) { throw 'Cannot insert a fresh synthetic ticket into dedicated test database.' }
} finally { $env:MYSQL_PWD = $previousPassword }
$headers = @{ 'X-Service-Key' = $env:E2E_SERVICE_KEY }
$invitation = Invoke-RestMethod -Method Post -Uri "$ApiBase/api/survey-invitations" -Headers $headers -ContentType 'application/json' -Body (@{ ticket_id = $fixtureId } | ConvertTo-Json)
$null = Invoke-RestMethod -Method Post -Uri "$ApiBase/api/survey-invitations/$($invitation.invitation_id)/send" -Headers $headers -ContentType 'application/json' -Body '{"channel":"SMS"}'
[pscustomobject]@{ Token = ($invitation.survey_url -split '/')[-1]; TicketCode = $fixtureCode; InvitationId = $invitation.invitation_id }
