# Smart CRM L8 frontend

Requires Node.js 22.12+ (Node.js 24 used for verification), backend at `http://127.0.0.1:8080` and MySQL configured as described in the repository README.

```powershell
cd frontend
npm ci
npm run dev
```

Open `http://127.0.0.1:5173`. `/api` is forwarded by Vite to the backend. Set `$env:VITE_API_TARGET='http://127.0.0.1:8081'` before starting Vite when the backend uses another port. Production hosting must serve `dist/` with an SPA fallback to `index.html` and proxy `/api` to Spring Boot. No service key or database credential belongs in frontend configuration.

The customer opens `/khao-sat/{token}`. Internal staff sign in at `/`; role navigation follows the API Contract. Customer responses cannot be edited or deleted. JWT session is kept in session storage for the current browser tab and removed on logout.

```powershell
npm test
npm run build
```

Unit/UI tests mock HTTP responses to exercise validation, error recovery, submission, filtering, pagination and navigation. They do not establish MySQL integration evidence.

## Real browser end-to-end test

Use a **disposable MySQL test schema**, start the backend with authentication bootstrap enabled and provision a fresh invitation for a closed ticket through the two internal invitation APIs. The test submits an immutable response and does not clean up or delete business records.

```powershell
npx playwright install chromium
$env:E2E_SERVICE_KEY = '<service key of dedicated test backend>'
$fixture = & ./e2e/prepare-fixture.ps1 -TestPort 3307 -ApiBase 'http://127.0.0.1:8080'
$env:E2E_SURVEY_TOKEN = $fixture.Token
$env:E2E_TICKET_CODE = $fixture.TicketCode
$env:E2E_PASSWORD = '<password configured for dedicated test bootstrap users>'
npm run test:e2e
```

Default users are `manager101`, `marketing`, `director`; override with `E2E_MANAGER_USERNAME`, `E2E_MARKETING_USERNAME`, `E2E_DIRECTOR_USERNAME` if needed. The backend must already be running. Playwright starts Vite automatically. Missing required environment variables fail the suite explicitly. Reruns need a fresh unused invitation because survey responses are immutable. `prepare-fixture.ps1` inserts a new synthetic ticket on each invocation, then calls the real create/send APIs; it neither deletes nor updates any existing data. It refuses port 3306 and schemas without the `_test` suffix. For another MySQL installation pass `-MySqlBin`; use `TEST_DB_PASSWORD` if the isolated server has a password.

The browser test uses real APIs to submit a response at 360px width, check duplicate prevention, view technician statistics and masked responses, view response rate, download Excel as Marketing, and view center statistics as a director. It also checks that the director cannot access response lookup. Screenshots are saved in ignored `test-results/`.
