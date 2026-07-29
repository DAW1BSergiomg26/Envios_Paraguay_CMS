# E2E + CI Guide — MonteAstur Envios

## Overview

This project uses two testing tools with clearly separated responsibilities:

| Tool       | Scope            | Command              | Config file                          |
|------------|------------------|----------------------|--------------------------------------|
| **Vitest** | Unit / component | `npm run test:unit`  | `vitest.config.js`                   |
| **Playwright** | E2E (browser) | `npm run test:e2e`   | `playwright.config.js`               |

- Vitest runs tests from `frontend-react/tests/` (jsdom, no browser needed)
- Playwright runs tests from `frontend-react/e2e/` (real Chromium browser)

---

## How to run E2E locally

### Prerequisites

- Docker Desktop running
- Node 20+ installed

### Option A: One-command runner (recommended)

```powershell
# Windows (PowerShell)
.\scripts\run-e2e-local.ps1
```

```bash
# Linux / WSL
./scripts/run-e2e-local.sh
```

This will:
1. `docker compose down` (clean state)
2. `docker compose up -d --build`
3. Wait for `http://localhost:8080/actuator/health` (UP, timeout 150s)
4. Wait for `http://localhost:8090` (nginx, timeout 30s)
5. Run `npm run test:e2e` against `http://localhost:8090`
6. Show clear pass/fail message

### Option B: Manual (Docker already running)

```powershell
cd frontend-react
$env:E2E_BASE_URL="http://localhost:8090"
npm run test:e2e
```

### Option C: Headed mode (watch tests in browser)

```powershell
cd frontend-react
npm run e2e:headed
```

### Option D: Playwright UI mode

```powershell
cd frontend-react
npm run e2e:ui
```

---

## How to interpret failures

1. **Playwright HTML report**: Open `frontend-react/playwright-report/index.html` in a browser.
   - Shows which tests passed/failed
   - Includes screenshots (captured on failure)
   - Includes trace viewer for failed tests (click "Trace" button)

2. **Traces**: `frontend-react/test-results/` contains trace files (`.zip`) with:
   - DOM snapshots before/after each action
   - Network requests
   - Console output
   - Time travel through the test

3. **Inspect traces**:
   ```bash
   npx playwright show-report frontend-react/playwright-report
   ```

4. **Docker logs**:
   ```bash
   docker compose logs app --tail 50
   docker compose logs nginx --tail 50
   ```

---

## Difference Vitest vs Playwright

| Aspect              | Vitest                          | Playwright                        |
|---------------------|---------------------------------|-----------------------------------|
| Browser needed?     | No (jsdom)                      | Yes (Chromium)                    |
| Speed               | Fast (~2s)                      | Slow (~20-60s)                    |
| What it tests       | Logic, components, hooks, utils | Full user flows in browser        |
| Real HTTP calls?    | No (mocked)                     | Yes (against real backend)        |
| CI cost             | Minimal                         | Higher (Docker + browser install) |
| Config file         | `vitest.config.js`              | `playwright.config.js`            |

**Rule of thumb:**
- If you can test it without a browser → Vitest
- If you need to verify a full user flow → Playwright

---

## How to activate E2E in CI

The E2E job is defined in `.github/workflows/ci.yml` under `e2e-tests`.

### Currently: manual trigger only

```yaml
if: github.event_name == 'workflow_dispatch'
```

To run it:
1. Go to **GitHub → Actions → CI → Run workflow**
2. Select branch
3. Click **Run workflow**

### To enable on every push (when ready)

Change the `if` condition:

```yaml
if: github.event_name == 'push' && github.ref == 'refs/heads/develop'
```

Or remove the `if` line entirely to run on every push/PR.

### CI job flow

1. `needs: [docker-build]` — images must be built first
2. Creates `.env` for CI
3. `docker compose up -d` (uses pre-built images from docker-build)
4. Waits for health (`actuator/health` → UP)
5. Installs Playwright + Chromium
6. Runs `npm run test:e2e` (against `http://localhost` — nginx on port 80 in CI)
7. If failure: uploads `playwright-report/` as artifact + prints Docker logs

### CI env variables for E2E

| Variable         | Value         |
|------------------|---------------|
| `E2E_BASE_URL`   | `http://localhost` |
| `E2E_ADMIN_USER` | `ci_admin`    |
| `E2E_ADMIN_PASS` | `ci_admin_pass` |

---

## Troubleshooting

### "Server not ready" / healthcheck timeout

```bash
# Check app logs
docker compose logs app --tail 50

# Check if MySQL is healthy
docker compose logs db --tail 20

# Verify health manually
curl http://localhost:8080/actuator/health
```

**Common causes:**
- MySQL not ready (wait for it)
- First build downloads Maven dependencies (slow)
- `.env` missing required vars
- Port conflict (8080 or 8090 already in use)

### Port 8090 already in use

```powershell
# Find what's using it
netstat -ano | findstr :8090

# Kill the process (replace PID)
Stop-Process -Id <PID> -Force
```

```bash
# Linux
lsof -i :8090
kill -9 <PID>
```

### Login tests fail (E2E)

1. Verify the app is reachable:
   ```bash
   curl http://localhost:8090/login-react
   ```
2. Check credentials: `admin` / `admin123` (default local `.env`)
3. For CI: `ci_admin` / `ci_admin_pass` (set in CI env)
4. Check CSRF: the login form requires a CSRF token. The SPA handles it automatically.

### Demo data missing

```bash
# Ensure APP_DEMO_DATA=true in .env
# Then rebuild:
docker compose down -v
docker compose up -d --build
```

### Nginx restart loop

```bash
docker compose logs nginx --tail 30
```

**Common causes:**
- Port already in use (change `NGINX_PORT` in `.env`)
- Syntax error in config (test with `docker compose exec nginx nginx -t`)

### Screenshots and traces

When E2E tests fail, Playwright automatically captures:
- **Screenshots** → `frontend-react/test-results/` (PNG files)
- **Traces** → `frontend-react/test-results/` (ZIP files, viewable in Playwright Trace Viewer)

To view them locally:
```bash
cd frontend-react
npx playwright show-trace test-results/<trace-file>.zip
```

These artifacts are:
- Ignored by Git (`.gitignore` already configured)
- Uploaded to GitHub Actions on failure (as `playwright-report` artifact)
- Configurable in `playwright.config.js` (`screenshot: 'only-on-failure'`, `trace: 'retain-on-failure'`)

---

## File structure

```
frontend-react/
├── e2e/                          # Playwright E2E tests
│   ├── home.spec.js              # Home page loads
│   ├── login.spec.js             # Login flow + CSRF handling
│   ├── tracking.spec.js          # Public tracking page
│   ├── dashboard.spec.js         # Admin dashboard after login
│   └── debug.spec.js             # Dev-only debug test (not for CI)
├── tests/                        # Vitest unit/component tests
├── playwright-report/            # Generated HTML report (gitignored)
├── test-results/                 # Screenshots + traces (gitignored)
├── playwright.config.js          # Playwright configuration
├── vitest.config.js              # Vitest configuration
└── package.json                  # Scripts: test:unit, test:e2e, test:all

scripts/
├── run-e2e-local.ps1             # Windows one-command E2E runner
└── run-e2e-local.sh              # Linux/WSL one-command E2E runner

.github/workflows/
└── ci.yml                        # CI pipeline (E2E = manual trigger)
```