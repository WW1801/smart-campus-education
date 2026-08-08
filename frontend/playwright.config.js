import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './tests/e2e',
  timeout: 30000,
  use: {
    baseURL: 'http://127.0.0.1:3012',
    browserName: 'chromium',
    channel: 'msedge',
    trace: 'retain-on-failure'
  },
  webServer: {
    command: 'npm run preview -- --host 127.0.0.1 --port 3012 --strictPort',
    url: 'http://127.0.0.1:3012',
    reuseExistingServer: true,
    timeout: 120000
  }
})
