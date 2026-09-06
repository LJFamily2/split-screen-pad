/**
 * Browser tests for the simulator, driven by Playwright against the Chromium
 * that ships with this environment.
 *
 *   node tests/simulator.test.js
 *
 * These exercise the behaviour a person actually touches: the divider (with
 * real pointer events, so touch support is covered), the ratio presets, swap,
 * rotate, full screen, the two- and three-pane layouts, the app picker, saved
 * pairs and the light/dark theme switch.
 */
const http = require('http');
const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');

const ROOT = path.join(__dirname, '..');
// Use the pre-installed Chromium when this environment ships one, otherwise let
// Playwright pick the browser it downloaded itself (as CI does).
const PREINSTALLED = '/opt/pw-browsers/chromium';
const CHROME = process.env.PLAYWRIGHT_CHROMIUM
  || (fs.existsSync(PREINSTALLED) ? PREINSTALLED : undefined);

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8'
};

let passed = 0;
const failures = [];

async function check(name, fn, page) {
  try {
    if (page) await reset(page);
    await fn();
    passed += 1;
    console.log(`  ok   ${name}`);
  } catch (error) {
    failures.push({ name, error });
    console.log(`  FAIL ${name}\n       ${error.message}`);
  }
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function assertClose(actual, expected, tolerance, message) {
  if (Math.abs(actual - expected) > tolerance) {
    throw new Error(`${message} (got ${actual}, expected ~${expected})`);
  }
}

function startServer() {
  const server = http.createServer((req, res) => {
    const urlPath = decodeURIComponent(req.url.split('?')[0]);
    const filePath = path.join(ROOT, urlPath === '/' ? 'index.html' : urlPath);
    if (!filePath.startsWith(ROOT) || !fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
      res.writeHead(404).end('not found');
      return;
    }
    res.writeHead(200, { 'Content-Type': MIME[path.extname(filePath)] || 'application/octet-stream' });
    fs.createReadStream(filePath).pipe(res);
  });
  return new Promise((resolve) => {
    server.listen(0, '127.0.0.1', () => resolve({ server, port: server.address().port }));
  });
}

/** Taps an element `times` in quick succession without actionability re-checks. */
async function tap(page, selector, times = 1) {
  const box = await page.locator(selector).boundingBox();
  const x = box.x + box.width / 2;
  const y = box.y + box.height / 2;
  await page.mouse.move(x, y);
  for (let i = 0; i < times; i += 1) {
    await page.mouse.down();
    await page.mouse.up();
  }
}

/** Puts the workspace back to a known state so checks stay independent. */
async function reset(page) {
  await closeDialogs(page);
  await page.evaluate(() => {
    const sp = window.__splitpad;
    if (sp.state.maximized !== 0) sp.toggleMaximize(sp.state.maximized);
    if (sp.state.paneCount !== 2) sp.setPaneCount(2);
    if (!sp.state.vertical) sp.setDirection(true);
    sp.applyRatio(0.5);
    sp.applySecondaryRatio(0.5);
  });
}

/** Closes whatever dialog a previous check left open. */
async function closeDialogs(page) {
  for (let i = 0; i < 3; i += 1) {
    const open = await page.locator('.modal:not([hidden])').count();
    if (open === 0) return;
    await page.keyboard.press('Escape');
    await page.waitForTimeout(60);
  }
}

/** Drags an element by (dx, dy) using pointer events, the way a finger would. */
async function dragBy(page, selector, dx, dy) {
  const box = await page.locator(selector).boundingBox();
  const x = box.x + box.width / 2;
  const y = box.y + box.height / 2;
  await page.mouse.move(x, y);
  await page.mouse.down();
  await page.mouse.move(x + dx / 2, y + dy / 2, { steps: 5 });
  await page.mouse.move(x + dx, y + dy, { steps: 5 });
  await page.mouse.up();
}

async function main() {
  const { server, port } = await startServer();
  const base = `http://127.0.0.1:${port}/simulator/index.html`;
  const browser = await chromium.launch({
    executablePath: CHROME,
    args: ['--disable-background-networking', '--no-first-run', '--disable-sync']
  });

  const consoleErrors = [];
  const missingRequests = [];
  const context = await browser.newContext({ viewport: { width: 1280, height: 820 } });
  context.setDefaultTimeout(6000);

  // Keep the run hermetic: nothing outside the local test server is fetched.
  await context.route('**/*', (route) => {
    if (route.request().url().startsWith('http://127.0.0.1:')) route.continue();
    else route.abort();
  });

  const page = await context.newPage();
  page.on('pageerror', (error) => consoleErrors.push(String(error)));
  page.on('console', (msg) => {
    if (msg.type() === 'error') consoleErrors.push(msg.text());
  });
  page.on('response', (response) => {
    if (response.status() >= 400) missingRequests.push(`${response.status()} ${response.url()}`);
  });
  page.on('requestfailed', (request) => {
    // Blocked external requests are expected; anything local is a real miss.
    if (request.url().startsWith('http://127.0.0.1:')) {
      missingRequests.push(`failed ${request.url()}`);
    }
  });

  await page.goto(base, { waitUntil: 'load' });
  await page.waitForFunction(() => Boolean(window.__splitpad));

  console.log('\nSimulator');

  await check('loads with no page or network errors', async () => {
    assert(consoleErrors.length === 0, `console errors: ${consoleErrors.join(' | ')}`);
    assert(missingRequests.length === 0, `404s: ${missingRequests.join(' | ')}`);
  }, page);

  await check('boots into a 50:50 split with both panes visible', async () => {
    const ratio = await page.evaluate(() => window.__splitpad.state.ratio);
    assertClose(ratio, 0.5, 0.01, 'initial ratio');
    assert(await page.locator('#pane-1').isVisible(), 'pane 1 visible');
    assert(await page.locator('#pane-2').isVisible(), 'pane 2 visible');
  }, page);

  await check('both panes render demo content, not a blank frame', async () => {
    for (const id of [1, 2]) {
      const frame = page.frameLocator(`#frame-${id}`);
      await frame.locator('h1').first().waitFor({ timeout: 5000 });
      const text = await frame.locator('h1').first().textContent();
      assert(text && text.trim().length > 0, `pane ${id} content`);
    }
  }, page);

  await check('ratio presets resize the split', async () => {
    await page.click('#btn-ratio-66');
    assertClose(await page.evaluate(() => window.__splitpad.state.ratio), 0.6667, 0.01, '2/3 preset');
    await page.click('#btn-ratio-33');
    assertClose(await page.evaluate(() => window.__splitpad.state.ratio), 0.3333, 0.01, '1/3 preset');
    await page.click('#btn-ratio-50');
    assertClose(await page.evaluate(() => window.__splitpad.state.ratio), 0.5, 0.01, '1/2 preset');
  }, page);

  await check('dragging the divider resizes the split (pointer events)', async () => {
    const before = await page.evaluate(() => window.__splitpad.state.ratio);
    await dragBy(page, '#divider', 0, 140);
    const after = await page.evaluate(() => window.__splitpad.state.ratio);
    assert(after > before + 0.05, `divider drag changed ratio (${before} -> ${after})`);
    await page.click('#btn-ratio-50');
  }, page);

  await check('dragging the divider to the edge full-screens a pane', async () => {
    await dragBy(page, '#divider', 0, -400);
    const maximized = await page.evaluate(() => window.__splitpad.state.maximized);
    assert(maximized === 2, `pane 2 full screen, got ${maximized}`);
    assert(await page.locator('#pane-1').isHidden(), 'pane 1 hidden while pane 2 is full screen');
    await page.keyboard.press('Escape');
    assert(await page.evaluate(() => window.__splitpad.state.maximized) === 0, 'Escape restores the split');
  }, page);

  await check('swap exchanges the two panes', async () => {
    const before = await page.evaluate(() => [window.__splitpad.panes[1].url, window.__splitpad.panes[2].url]);
    await page.click('#btn-swap');
    const after = await page.evaluate(() => [window.__splitpad.panes[1].url, window.__splitpad.panes[2].url]);
    assert(after[0] === before[1] && after[1] === before[0], `swap (${before} -> ${after})`);
  }, page);

  await check('the divider menu opens immediately, with no double-tap delay', async () => {
    await closeDialogs(page);
    const started = Date.now();
    await tap(page, '#divider');
    await page.waitForSelector('#action-modal:not([hidden])');
    const elapsed = Date.now() - started;
    assert(elapsed < 250, `sheet opened in ${elapsed}ms`);
    await page.keyboard.press('Escape');
  }, page);

  await check('tapping the divider opens the split options menu', async () => {
    await closeDialogs(page);
    await tap(page, '#divider');
    await page.waitForSelector('#action-modal:not([hidden])', { timeout: 2000 });
    const rows = await page.locator('#action-list .action-row').count();
    assert(rows >= 6, `split options rows: ${rows}`);
    await page.keyboard.press('Escape');
  }, page);

  await check('rotate switches between stacked and side-by-side', async () => {
    await closeDialogs(page);
    await page.click('#btn-rotate');
    assert(await page.locator('#split.horizontal').count() === 1, 'horizontal after rotate');
    await page.click('#btn-rotate');
    assert(await page.locator('#split.vertical').count() === 1, 'vertical after rotating back');
  }, page);

  await check('divider drag works along the horizontal axis too', async () => {
    await page.click('#btn-rotate');
    const before = await page.evaluate(() => window.__splitpad.state.ratio);
    await dragBy(page, '#divider', 90, 0);
    const after = await page.evaluate(() => window.__splitpad.state.ratio);
    assert(after > before + 0.05, `horizontal drag (${before} -> ${after})`);
    await page.click('#btn-rotate');
    await page.click('#btn-ratio-50');
  }, page);

  await check('the panel menu offers full screen and a move-to action', async () => {
    await page.click('[data-menu="1"]');
    await page.waitForSelector('#action-modal:not([hidden])');
    const labels = await page.locator('#action-list .ar-label').allTextContents();
    assert(labels.some((l) => /full screen/i.test(l)), 'full screen action');
    assert(labels.some((l) => /^Move to /.test(l)), `move action, got ${labels.join(' | ')}`);
    assert(labels.some((l) => /third pane/i.test(l)), 'add-a-pane action');
    await page.keyboard.press('Escape');
  }, page);

  await check('the two-step app picker fills both panes', async () => {
    await page.click('#btn-apps');
    await page.waitForSelector('#picker-modal:not([hidden])');
    assert((await page.locator('#picker-step').textContent()).includes('Step 1'), 'starts at step 1');
    await page.locator('#picker-grid .app-card', { hasText: 'Reader' }).first().click();
    assert((await page.locator('#picker-step').textContent()).includes('Step 2'), 'advances to step 2');
    await page.locator('#picker-grid .app-card', { hasText: 'Notes' }).first().click();
    await page.waitForSelector('#picker-modal', { state: 'hidden' });
    const urls = await page.evaluate(() => [window.__splitpad.panes[1].url, window.__splitpad.panes[2].url]);
    assert(urls[0].includes('reader') && urls[1].includes('notes'), `picker result: ${urls}`);
  }, page);

  await check('picker search filters the list', async () => {
    await page.click('#btn-apps');
    await page.fill('#picker-search', 'wikipedia');
    const count = await page.locator('#picker-grid .app-card').count();
    assert(count === 1, `filtered to ${count} cards`);
    await page.fill('#picker-search', 'zzzznotathing');
    assert(await page.locator('#picker-grid .empty-note').count() === 1, 'empty state shown');
    await page.keyboard.press('Escape');
  }, page);

  await check('saved pairs launch into both panes', async () => {
    await page.click('#btn-pairs');
    await page.waitForSelector('#pairs-modal:not([hidden])');
    const rows = await page.locator('.pair-row').count();
    assert(rows >= 3, `seeded pairs: ${rows}`);
    await page.locator('.pair-row', { hasText: 'Player + Notes' }).locator('[data-act="launch"]').click();
    await page.waitForSelector('#pairs-modal', { state: 'hidden' });
    const urls = await page.evaluate(() => [window.__splitpad.panes[1].url, window.__splitpad.panes[2].url]);
    assert(urls[0].includes('player') && urls[1].includes('notes'), `pair loaded: ${urls}`);
  }, page);

  await check('deleting a pair removes it', async () => {
    await page.click('#btn-pairs');
    const before = await page.locator('.pair-row').count();
    await page.locator('.pair-row').first().locator('[data-act="delete"]').click();
    const after = await page.locator('.pair-row').count();
    assert(after === before - 1, `pairs ${before} -> ${after}`);
    await page.keyboard.press('Escape');
  }, page);

  await check('sites that block embedding show an explanation, not a blank pane', async () => {
    await page.fill('#url-1', 'youtube.com');
    await page.press('#url-1', 'Enter');
    await page.waitForSelector('#block-1.show');
    const text = await page.locator('#block-text-1').textContent();
    assert(/X-Frame-Options/.test(text), 'explains why the frame is blocked');
    await page.evaluate(() => window.__splitpad.loadPane(1, 'pages/player.html'));
  }, page);

  await check('typing words in the address bar becomes a search', async () => {
    const url = await page.evaluate(() => window.__splitpad.normalizeUrl('split screen tips'));
    assert(url.startsWith('https://www.google.com/search?q='), `search fallback: ${url}`);
    const direct = await page.evaluate(() => window.__splitpad.normalizeUrl('example.com/path'));
    assert(direct === 'https://example.com/path', `domain handling: ${direct}`);
  }, page);

  await check('desktop mode toggles per pane', async () => {
    await page.click('#ua-1');
    assert(await page.evaluate(() => window.__splitpad.panes[1].desktop) === true, 'pane 1 desktop on');
    assert(await page.evaluate(() => window.__splitpad.panes[2].desktop) === false, 'pane 2 untouched');
    assert((await page.locator('#ua-1').textContent()).trim() === 'Desktop', 'label updates');
    await page.click('#ua-1');
  }, page);

  await check('switching to three panes gives one half plus two stacked', async () => {
    await page.click('#btn-layout-3');
    const st = await page.evaluate(() => window.__splitpad.state);
    assert(st.paneCount === 3, `pane count ${st.paneCount}`);
    assert(st.vertical === false, 'primary split runs left/right for three panes');
    for (const id of [1, 2, 3]) {
      assert(await page.locator(`#pane-${id}`).isVisible(), `pane ${id} visible`);
    }

    // Pane 1 spans the full height of one half; 2 and 3 share the other half.
    const [b1, b2, b3] = await Promise.all(
      [1, 2, 3].map((id) => page.locator(`#pane-${id}`).boundingBox())
    );
    assert(b1.height > b2.height * 1.5, `pane 1 is full height (${b1.height} vs ${b2.height})`);
    assert(Math.abs(b2.x - b3.x) < 2, 'panes 2 and 3 share a column');
    assert(b3.y > b2.y + b2.height - 2, 'pane 3 sits below pane 2');
    assert(b2.x > b1.x + b1.width - 40, 'the stacked pair is in the other half');
  }, page);

  await check('the third pane loads its own page', async () => {
    await page.click('#btn-layout-3');
    const frame = page.frameLocator('#frame-3');
    await frame.locator('h1').first().waitFor({ timeout: 5000 });
    const text = await frame.locator('h1').first().textContent();
    assert(text && text.trim().length > 0, 'pane 3 content');
  }, page);

  await check('the second divider resizes only the stacked pair', async () => {
    await page.click('#btn-layout-3');
    const before = await page.evaluate(() => window.__splitpad.state);
    await dragBy(page, '#divider-2', 0, 90);
    const after = await page.evaluate(() => window.__splitpad.state);
    assert(after.secondaryRatio > before.secondaryRatio + 0.05,
      `secondary ratio ${before.secondaryRatio} -> ${after.secondaryRatio}`);
    assertClose(after.ratio, before.ratio, 0.01, 'primary ratio untouched');
  }, page);

  await check('the panel menu moves a pane between slots', async () => {
    await page.click('#btn-layout-3');
    const before = await page.evaluate(() => [
      window.__splitpad.panes[1].url, window.__splitpad.panes[3].url
    ]);
    await page.click('[data-menu="1"]');
    await page.waitForSelector('#action-modal:not([hidden])');
    await page.locator('.action-row', { hasText: 'Move to Bottom right' }).click();
    const after = await page.evaluate(() => [
      window.__splitpad.panes[1].url, window.__splitpad.panes[3].url
    ]);
    assert(after[0] === before[1] && after[2 - 1] === before[0],
      `moved pane 1 to the bottom right (${before} -> ${after})`);
  }, page);

  await check('slot names follow the layout', async () => {
    const names = await page.evaluate(() => {
      const sp = window.__splitpad;
      sp.setPaneCount(2);
      sp.setDirection(true);
      const twoStacked = [sp.positionName(1), sp.positionName(2)];
      sp.setPaneCount(3);
      const three = [sp.positionName(1), sp.positionName(2), sp.positionName(3)];
      return { twoStacked, three };
    });
    assert(names.twoStacked.join() === 'Top,Bottom', names.twoStacked.join());
    assert(names.three.join() === 'Left half,Top right,Bottom right', names.three.join());
  }, page);

  await check('closing a pane returns to two and keeps the survivors', async () => {
    await page.click('#btn-layout-3');
    await page.evaluate(() => {
      window.__splitpad.loadPane(1, 'pages/player.html');
      window.__splitpad.loadPane(2, 'pages/notes.html');
      window.__splitpad.loadPane(3, 'pages/reader.html');
    });
    await page.click('[data-menu="2"]');
    await page.waitForSelector('#action-modal:not([hidden])');
    await page.locator('.action-row', { hasText: 'Close this pane' }).click();
    const st = await page.evaluate(() => ({
      count: window.__splitpad.state.paneCount,
      urls: [window.__splitpad.panes[1].url, window.__splitpad.panes[2].url]
    }));
    assert(st.count === 2, `back to ${st.count} panes`);
    assert(st.urls[0].includes('player'), `pane 1 kept: ${st.urls[0]}`);
    assert(st.urls[1].includes('reader'), `pane 3 moved up: ${st.urls[1]}`);
    assert(await page.locator('#pane-3').isHidden(), 'third pane hidden');
  }, page);

  await check('the pane count survives a reload', async () => {
    await page.click('#btn-layout-3');
    await page.reload({ waitUntil: 'load' });
    await page.waitForFunction(() => Boolean(window.__splitpad));
    const st = await page.evaluate(() => window.__splitpad.state);
    assert(st.paneCount === 3, `restored ${st.paneCount} panes`);
    assert(await page.locator('#pane-3').isVisible(), 'pane 3 restored');
  }, page);

  await check('the floating window is gone', async () => {
    assert(await page.locator('#floating').count() === 0, 'no floating window element');
    assert(await page.locator('#btn-floating').count() === 0, 'no floating toolbar button');
    const labels = await page.evaluate(() =>
      Array.from(document.querySelectorAll('button')).map((b) => b.textContent).join(' '));
    assert(!/float/i.test(labels), 'no floating controls remain');
  }, page);

  await check('keyboard shortcuts drive the split', async () => {
    await page.keyboard.press('Control+d');
    assert(await page.locator('#split.horizontal').count() === 1, 'Ctrl+D rotates');
    await page.keyboard.press('Control+d');
    await page.keyboard.press('Control+2');
    assert(await page.evaluate(() => window.__splitpad.state.active) === 2, 'Ctrl+2 focuses pane 2');
    await page.keyboard.press('Control+m');
    assert(await page.evaluate(() => window.__splitpad.state.maximized) === 2, 'Ctrl+M full screens');
    await page.keyboard.press('Escape');
  }, page);

  await check('the theme toggle cycles device / light / dark', async () => {
    assert(await page.evaluate(() => document.documentElement.hasAttribute('data-theme')) === false,
      'starts on the device theme');
    await page.click('#btn-theme');
    assert(await page.getAttribute('html', 'data-theme') === 'light', 'light');
    await page.click('#btn-theme');
    assert(await page.getAttribute('html', 'data-theme') === 'dark', 'dark');
    await page.click('#btn-theme');
    assert(await page.evaluate(() => document.documentElement.hasAttribute('data-theme')) === false,
      'back to the device theme');
  }, page);

  await check('the layout follows the OS dark preference', async () => {
    const darkPage = await context.newPage();
    darkPage.setDefaultTimeout(6000);
    await darkPage.emulateMedia({ colorScheme: 'dark' });
    await darkPage.goto(base, { waitUntil: 'load' });
    const bg = await darkPage.evaluate(() =>
      getComputedStyle(document.body).backgroundColor);
    const rgb = bg.match(/\d+/g).map(Number);
    assert(rgb[0] + rgb[1] + rgb[2] < 200, `dark background, got ${bg}`);
    await darkPage.close();
  }, page);

  await check('the session is restored after a reload', async () => {
    await page.evaluate(() => window.__splitpad.loadPane(1, 'pages/reader.html'));
    await page.evaluate(() => window.__splitpad.applyRatio(0.68));
    await page.reload({ waitUntil: 'load' });
    await page.waitForFunction(() => Boolean(window.__splitpad));
    const ratio = await page.evaluate(() => window.__splitpad.state.ratio);
    const url = await page.evaluate(() => window.__splitpad.panes[1].url);
    assertClose(ratio, 0.68, 0.02, 'ratio restored');
    assert(url.includes('reader'), `pane 1 restored: ${url}`);
  }, page);

  await check('works at a small tablet viewport without horizontal overflow', async () => {
    const small = await context.newPage();
    small.setDefaultTimeout(6000);
    await small.setViewportSize({ width: 800, height: 1340 });
    await small.goto(base, { waitUntil: 'load' });
    await small.waitForFunction(() => Boolean(window.__splitpad));
    const overflow = await small.evaluate(() =>
      document.documentElement.scrollWidth - document.documentElement.clientWidth);
    assert(overflow <= 1, `no horizontal overflow, got ${overflow}px`);
    assert(await small.locator('#pane-1').isVisible() && await small.locator('#pane-2').isVisible(),
      'both panes visible at 800px');
    await small.close();
  }, page);

  await browser.close();
  server.close();

  console.log(`\n${passed} passed, ${failures.length} failed`);
  process.exit(failures.length ? 1 : 0);
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
