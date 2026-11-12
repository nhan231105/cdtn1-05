$ErrorActionPreference='Stop'
$root=(Get-Location).Path
$evidence=Join-Path $root 'docs/evidence/web-acceptance'
New-Item -ItemType Directory -Force -Path $evidence | Out-Null
foreach ($requiredPort in @(8080,5173)) { if (Get-NetTCPConnection -LocalPort $requiredPort -State Listen -ErrorAction SilentlyContinue) { throw "Port $requiredPort is already in use; stop your demo first." } }
$database=& ./scripts/start-test-db.ps1
$database | ConvertTo-Json | Set-Content -Encoding UTF8 (Join-Path $evidence 'test-environment.json')
$env:DB_URL='jdbc:mysql://127.0.0.1:3307/smartcrm_l8_test?connectionTimeZone=Asia/Ho_Chi_Minh'
$env:DB_USER='root'
$env:BOOTSTRAP_ENABLED='true'
$env:SURVEY_SCHEDULER_ENABLED='false'
$env:GATEWAY_SIMULATE_FAILURE='false'
$env:SURVEY_PUBLIC_BASE_URL='http://127.0.0.1:5173/khao-sat'
$env:JWT_SECRET=[guid]::NewGuid().ToString('N')+[guid]::NewGuid().ToString('N')
$env:SERVICE_API_KEY=[guid]::NewGuid().ToString('N')+[guid]::NewGuid().ToString('N')
$env:BOOTSTRAP_PASSWORD=[guid]::NewGuid().ToString('N')
$env:E2E_PASSWORD=$env:BOOTSTRAP_PASSWORD
$env:E2E_SERVICE_KEY=$env:SERVICE_API_KEY
$backend=$null
$vite=$null
try {
  $backend=Start-Process -FilePath 'cmd.exe' -ArgumentList @('/c','mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.datasource.password="') -WorkingDirectory (Join-Path $root 'backend') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $evidence 'backend-startup.log') -RedirectStandardError (Join-Path $evidence 'backend-stderr.log')
  $ready=$false
  for($i=0;$i -lt 120;$i++){
    try { $null=Invoke-WebRequest -UseBasicParsing http://127.0.0.1:8080/api/auth/me -TimeoutSec 1 }
    catch { if($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq 401){$ready=$true;break} }
    Start-Sleep -Milliseconds 500
  }
  if(!$ready){throw 'Backend startup timeout; inspect backend-startup.log'}
  & 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe' --no-defaults --host=127.0.0.1 --port=3307 --user=root --database=smartcrm_l8_test --execute="SELECT COUNT(*) AS invitations_before FROM survey_invitation; SELECT COUNT(*) AS responses_before FROM survey_response;" | Set-Content -Encoding UTF8 (Join-Path $evidence 'mysql-before.txt')
  $fixture=& ./frontend/e2e/prepare-fixture.ps1
  $env:E2E_SURVEY_TOKEN=$fixture.Token
  $env:E2E_TICKET_CODE=$fixture.TicketCode
  $env:ACCEPTANCE_TICKET_CODE=$fixture.TicketCode
  $vite=Start-Process -FilePath 'cmd.exe' -ArgumentList @('/c','npm run dev') -WorkingDirectory (Join-Path $root 'frontend') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $evidence 'frontend-startup.log') -RedirectStandardError (Join-Path $evidence 'frontend-stderr.log')
  Push-Location frontend
  try {
    & cmd /c "npm run test:e2e 2>&1" | Set-Content -Encoding UTF8 (Join-Path $evidence 'e2e.log')
    if($LASTEXITCODE -ne 0){throw 'Existing E2E failed; inspect e2e.log'}
  } finally {Pop-Location}
  & cmd /c "node ./docs/evidence/web-acceptance/browser-check.mjs 2>&1" | Set-Content -Encoding UTF8 (Join-Path $evidence 'browser-run.log')
  if($LASTEXITCODE -ne 0){throw 'Acceptance browser checks failed'}
  & 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe' --no-defaults --host=127.0.0.1 --port=3307 --user=root --database=smartcrm_l8_test --table --execute="SELECT version,description,success FROM flyway_schema_history; SELECT t.ticket_code,i.status,i.send_attempts,r.score,r.comment,COUNT(r.response_id) AS response_count FROM ticket t JOIN survey_invitation i USING(ticket_id) LEFT JOIN survey_response r USING(ticket_id) GROUP BY t.ticket_id,i.invitation_id,r.response_id; SELECT TABLE_NAME,CONSTRAINT_NAME,CONSTRAINT_TYPE FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA=DATABASE(); SELECT ticket_id,COUNT(*) FROM survey_response GROUP BY ticket_id HAVING COUNT(*)>1;" | Set-Content -Encoding UTF8 (Join-Path $evidence 'mysql-after.txt')
  Copy-Item frontend/test-results/*/*.png -Destination $evidence -ErrorAction SilentlyContinue
} finally {
  if($vite){ & taskkill /PID $vite.Id /T /F | Out-Null }
  if($backend){ & taskkill /PID $backend.Id /T /F | Out-Null }
  & 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqladmin.exe' --no-defaults --host=127.0.0.1 --port=3307 --user=root shutdown
}




