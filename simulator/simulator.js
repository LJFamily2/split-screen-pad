/* =============================================================================
   Split Pad simulator
   Mirrors the Android app: ratio presets, a draggable divider with drag-to-edge
   full screen, double-tap swap, rotate, per-pane navigation and desktop mode,
   saved app pairs, a two-step app picker, and a floating window that can be
   dragged, resized and minimised to a bubble.

   Everything uses Pointer Events, so the same code path serves mouse, touch and
   pen — the previous build was mouse-only and did nothing on a tablet.
   ============================================================================= */
(() => {
  'use strict';

  const MIN_RATIO = 0.15;
  const MAX_RATIO = 0.85;
  const EDGE_SNAP = 0.18;
  const DOUBLE_TAP_MS = 280;
  const STORE = {
    pairs: 'splitpad.pairs',
    session: 'splitpad.session',
    theme: 'splitpad.theme'
  };

  /* ------------------------------------------------------------------ data */

  // Sites that refuse to be framed. In the Android app these load fine in a
  // WebView; in a browser we have to say so rather than showing a blank pane.
  const FRAME_BLOCKED = [
    'google.com', 'youtube.com', 'facebook.com', 'instagram.com', 'x.com',
    'twitter.com', 'chatgpt.com', 'openai.com', 'reddit.com', 'tiktok.com',
    'netflix.com', 'spotify.com', 'github.com', 'shopee.vn', 'zalo.me',
    'messenger.com', 'telegram.org', 'wikipedia.org', 'docs.google.com',
    'news.google.com', 'translate.google.com'
  ];

  const DEMO_PAGES = [
    { id: 'player', name: 'Media player', icon: '▶️', url: 'pages/player.html' },
    { id: 'notes', name: 'Notes', icon: '📝', url: 'pages/notes.html' },
    { id: 'reader', name: 'Reader', icon: '📚', url: 'pages/reader.html' }
  ];

  const WEB_APPS = [
    { id: 'google', name: 'Google', icon: '🌐', url: 'https://www.google.com' },
    { id: 'youtube', name: 'YouTube', icon: '▶️', url: 'https://m.youtube.com' },
    { id: 'chatgpt', name: 'ChatGPT', icon: '🤖', url: 'https://chatgpt.com' },
    { id: 'wikipedia', name: 'Wikipedia', icon: '📚', url: 'https://www.wikipedia.org' },
    { id: 'docs', name: 'Google Docs', icon: '📄', url: 'https://docs.google.com' },
    { id: 'maps', name: 'Google Maps', icon: '🗺️', url: 'https://maps.google.com' },
    { id: 'zalo', name: 'Zalo Web', icon: '💬', url: 'https://chat.zalo.me' },
    { id: 'facebook', name: 'Facebook', icon: '👤', url: 'https://m.facebook.com' },
    { id: 'reddit', name: 'Reddit', icon: '🔴', url: 'https://www.reddit.com' },
    { id: 'news', name: 'Google News', icon: '📰', url: 'https://news.google.com' },
    { id: 'tiktok', name: 'TikTok', icon: '🎵', url: 'https://www.tiktok.com' },
    { id: 'example', name: 'example.com', icon: '🧪', url: 'https://example.com' }
  ];

  const ALL_APPS = [...DEMO_PAGES, ...WEB_APPS];

  const DEFAULT_PAIRS = [
    { id: 'seed-1', title: 'Player + Notes', p1: 'pages/player.html', p2: 'pages/notes.html' },
    { id: 'seed-2', title: 'Reader + Notes', p1: 'pages/reader.html', p2: 'pages/notes.html' },
    { id: 'seed-3', title: 'YouTube + Google', p1: 'https://m.youtube.com', p2: 'https://www.google.com' }
  ];

  /* ------------------------------------------------------------- utilities */

  const $ = (id) => document.getElementById(id);
  const qsa = (sel) => Array.from(document.querySelectorAll(sel));

  const store = {
    read(key, fallback) {
      try {
        const raw = localStorage.getItem(key);
        return raw ? JSON.parse(raw) : fallback;
      } catch (err) {
        return fallback;
      }
    },
    write(key, value) {
      try {
        localStorage.setItem(key, JSON.stringify(value));
      } catch (err) {
        /* private mode — the simulator still works, it just forgets */
      }
    }
  };

  function isLocalPage(url) {
    return /^pages\/[\w-]+\.html$/.test(String(url).trim());
  }

  function normalizeUrl(input) {
    const value = String(input || '').trim();
    if (!value) return 'https://www.google.com';
    if (isLocalPage(value)) return value;
    if (/^https?:\/\//i.test(value)) return value;
    if (/^\/\//.test(value)) return `https:${value}`;
    if (/^[\w-]+(\.[\w-]+)+(\/.*)?$/.test(value)) return `https://${value}`;
    return `https://www.google.com/search?q=${encodeURIComponent(value)}`;
  }

  function prettyUrl(url) {
    return String(url).replace(/^https?:\/\//, '').replace(/^www\./, '').replace(/\/$/, '');
  }

  function hostOf(url) {
    if (isLocalPage(url)) {
      const app = ALL_APPS.find((a) => a.url === url);
      return app ? app.name : url;
    }
    try {
      return new URL(url).hostname.replace(/^www\./, '');
    } catch (err) {
      return prettyUrl(url);
    }
  }

  function isFrameBlocked(url) {
    if (isLocalPage(url)) return false;
    const host = hostOf(url);
    return FRAME_BLOCKED.some((blocked) => host === blocked || host.endsWith(`.${blocked}`));
  }

  let toastTimer = null;
  function toast(message) {
    const el = $('toast');
    el.textContent = message;
    el.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => el.classList.remove('show'), 2200);
  }

  /* ----------------------------------------------------------------- theme */

  const themeButton = $('btn-theme');
  const THEME_ORDER = ['auto', 'light', 'dark'];
  const THEME_LABEL = {
    auto: { icon: '🌗', text: 'Theme: follows your device' },
    light: { icon: '☀️', text: 'Theme: light' },
    dark: { icon: '🌙', text: 'Theme: dark' }
  };

  function applyTheme(mode) {
    if (mode === 'auto') {
      document.documentElement.removeAttribute('data-theme');
    } else {
      document.documentElement.setAttribute('data-theme', mode);
    }
    themeButton.textContent = THEME_LABEL[mode].icon;
    themeButton.title = THEME_LABEL[mode].text;
    store.write(STORE.theme, mode);
  }

  let themeMode = store.read(STORE.theme, 'auto');
  if (!THEME_ORDER.includes(themeMode)) themeMode = 'auto';
  applyTheme(themeMode);

  themeButton.addEventListener('click', () => {
    themeMode = THEME_ORDER[(THEME_ORDER.indexOf(themeMode) + 1) % THEME_ORDER.length];
    applyTheme(themeMode);
    toast(THEME_LABEL[themeMode].text);
  });

  /* ------------------------------------------------------------------ panes */

  const splitEl = $('split');
  const dividerEl = $('divider');
  const ratioTip = $('ratio-tip');

  const panes = {
    1: makePane(1),
    2: makePane(2)
  };

  function makePane(id) {
    return {
      id,
      root: $(`pane-${id}`),
      frame: $(`frame-${id}`),
      urlField: $(`url-${id}`),
      progress: $(`progress-${id}`),
      uaButton: $(`ua-${id}`),
      block: $(`block-${id}`),
      blockTitle: $(`block-title-${id}`),
      blockText: $(`block-text-${id}`),
      url: '',
      desktop: false,
      history: [],
      historyIndex: -1
    };
  }

  const state = {
    ratio: 0.5,
    vertical: true,
    maximized: 0,
    active: 1,
    floating: { url: 'pages/reader.html', alphaStep: 0, minimized: false }
  };

  function saveSession() {
    store.write(STORE.session, {
      p1: panes[1].url,
      p2: panes[2].url,
      ratio: state.ratio,
      vertical: state.vertical,
      d1: panes[1].desktop,
      d2: panes[2].desktop
    });
  }

  function loadPane(id, rawUrl, { pushHistory = true } = {}) {
    const pane = panes[id];
    const url = normalizeUrl(rawUrl);
    pane.url = url;
    pane.urlField.value = isLocalPage(url) ? hostOf(url) : prettyUrl(url);

    if (isFrameBlocked(url)) {
      pane.frame.removeAttribute('src');
      pane.blockTitle.textContent = `${hostOf(url)} blocks embedding`;
      pane.blockText.textContent =
        `${hostOf(url)} sends an X-Frame-Options header, so no browser will show it inside ` +
        'another page. The Android app loads it in a real WebView, where this restriction ' +
        'does not apply. Try one of the demo pages to see the split working end to end.';
      pane.block.classList.add('show');
      pane.progress.classList.remove('loading');
    } else {
      pane.block.classList.remove('show');
      startProgress(pane);
      pane.frame.src = url;
    }

    if (pushHistory) {
      pane.history = pane.history.slice(0, pane.historyIndex + 1);
      pane.history.push(url);
      pane.historyIndex = pane.history.length - 1;
    }
    updateNavButtons(id);
    setActivePane(id);
    saveSession();
  }

  function startProgress(pane) {
    pane.progress.classList.add('loading');
    pane.progress.style.width = '18%';
    requestAnimationFrame(() => { pane.progress.style.width = '72%'; });
  }

  function finishProgress(pane) {
    pane.progress.style.width = '100%';
    setTimeout(() => {
      pane.progress.classList.remove('loading');
      pane.progress.style.width = '0%';
    }, 260);
  }

  [1, 2].forEach((id) => {
    const pane = panes[id];
    pane.frame.addEventListener('load', () => finishProgress(pane));

    pane.urlField.addEventListener('keydown', (event) => {
      if (event.key === 'Enter') loadPane(id, pane.urlField.value);
    });
    pane.urlField.addEventListener('focus', () => setActivePane(id));
    pane.root.addEventListener('pointerdown', () => setActivePane(id));

    pane.uaButton.addEventListener('click', () => setDesktopMode(id, !pane.desktop));
  });

  qsa('[data-focus]').forEach((el) => {
    el.addEventListener('click', () => setActivePane(Number(el.dataset.focus)));
  });

  qsa('[data-nav]').forEach((el) => {
    el.addEventListener('click', () => {
      const id = Number(el.dataset.pane);
      const pane = panes[id];
      setActivePane(id);
      if (el.dataset.nav === 'reload') {
        loadPane(id, pane.url, { pushHistory: false });
      } else if (el.dataset.nav === 'back' && pane.historyIndex > 0) {
        pane.historyIndex -= 1;
        loadPane(id, pane.history[pane.historyIndex], { pushHistory: false });
      } else if (el.dataset.nav === 'forward' && pane.historyIndex < pane.history.length - 1) {
        pane.historyIndex += 1;
        loadPane(id, pane.history[pane.historyIndex], { pushHistory: false });
      }
      updateNavButtons(id);
    });
  });

  qsa('[data-open-tab]').forEach((el) => {
    el.addEventListener('click', () => {
      const pane = panes[Number(el.dataset.openTab)];
      window.open(pane.url, '_blank', 'noopener');
    });
  });

  qsa('[data-menu]').forEach((el) => {
    el.addEventListener('click', () => showPaneMenu(Number(el.dataset.menu)));
  });

  function updateNavButtons(id) {
    const pane = panes[id];
    const back = document.querySelector(`[data-nav="back"][data-pane="${id}"]`);
    const forward = document.querySelector(`[data-nav="forward"][data-pane="${id}"]`);
    if (back) back.disabled = pane.historyIndex <= 0;
    if (forward) forward.disabled = pane.historyIndex >= pane.history.length - 1;
  }

  function setActivePane(id) {
    state.active = id;
    panes[1].root.classList.toggle('active', id === 1);
    panes[2].root.classList.toggle('active', id === 2);
  }

  function setDesktopMode(id, desktop) {
    const pane = panes[id];
    pane.desktop = desktop;
    pane.uaButton.textContent = desktop ? 'Desktop' : 'Mobile';
    pane.uaButton.classList.toggle('desktop', desktop);
    // A desktop site in a narrow pane is shown zoomed out, the way the WebView
    // does it with useWideViewPort + loadWithOverviewMode.
    pane.frame.style.width = desktop ? '1280px' : '100%';
    pane.frame.style.height = desktop ? '1024px' : '100%';
    pane.frame.style.transformOrigin = 'top left';
    if (desktop) {
      const rect = pane.frame.parentElement.getBoundingClientRect();
      const scale = Math.max(rect.width / 1280, 0.2);
      pane.frame.style.transform = `scale(${scale})`;
    } else {
      pane.frame.style.transform = '';
    }
    saveSession();
  }

  function rescaleDesktopFrames() {
    [1, 2].forEach((id) => { if (panes[id].desktop) setDesktopMode(id, true); });
  }

  /* ------------------------------------------------------------------ split */

  function applyRatio(ratio, { persist = true } = {}) {
    if (state.maximized !== 0) restorePanes();
    state.ratio = Math.min(MAX_RATIO, Math.max(MIN_RATIO, ratio));
    panes[1].root.style.flex = `${state.ratio} 1 0%`;
    panes[2].root.style.flex = `${1 - state.ratio} 1 0%`;
    const left = Math.round(state.ratio * 100);
    ratioTip.textContent = `${left} : ${100 - left}`;
    if (persist) saveSession();
    rescaleDesktopFrames();
  }

  function maximizePane(id) {
    if (state.ratio <= MIN_RATIO + 0.02 || state.ratio >= MAX_RATIO - 0.02) state.ratio = 0.5;
    state.maximized = id;
    panes[1].root.classList.toggle('hidden-pane', id !== 1);
    panes[2].root.classList.toggle('hidden-pane', id !== 2);
    panes[id].root.style.flex = '1 1 0%';
    setActivePane(id);
    toast('Full screen — drag the divider or press Escape to return');
    rescaleDesktopFrames();
  }

  function restorePanes() {
    if (state.maximized === 0) return;
    state.maximized = 0;
    panes[1].root.classList.remove('hidden-pane');
    panes[2].root.classList.remove('hidden-pane');
    panes[1].root.style.flex = `${state.ratio} 1 0%`;
    panes[2].root.style.flex = `${1 - state.ratio} 1 0%`;
    rescaleDesktopFrames();
  }

  function toggleMaximize(id) {
    if (state.maximized === id) restorePanes(); else maximizePane(id);
  }

  function setDirection(vertical) {
    state.vertical = vertical;
    splitEl.classList.toggle('vertical', vertical);
    splitEl.classList.toggle('horizontal', !vertical);
    saveSession();
    rescaleDesktopFrames();
  }

  function swapPanes() {
    const a = panes[1].url;
    const b = panes[2].url;
    const da = panes[1].desktop;
    const db = panes[2].desktop;
    loadPane(1, b);
    loadPane(2, a);
    setDesktopMode(1, db);
    setDesktopMode(2, da);
    toast('Panes swapped');
  }

  /* --------------------------------------------------- divider interaction */

  let dragging = false;
  let dragStartPos = 0;
  let dragStartRatio = 0.5;
  let moved = false;
  let lastTapTime = 0;

  dividerEl.addEventListener('pointerdown', (event) => {
    dragging = true;
    moved = false;
    dragStartPos = state.vertical ? event.clientY : event.clientX;
    dragStartRatio = state.ratio;
    dividerEl.setPointerCapture(event.pointerId);
    dividerEl.classList.add('dragging');
  });

  dividerEl.addEventListener('pointermove', (event) => {
    if (!dragging) return;
    const pos = state.vertical ? event.clientY : event.clientX;
    if (!moved && Math.abs(pos - dragStartPos) > 4) {
      moved = true;
      if (state.maximized !== 0) {
        restorePanes();
        dragStartRatio = state.ratio;
        dragStartPos = pos;
      }
    }
    if (!moved) return;
    const rect = splitEl.getBoundingClientRect();
    const dividerSize = state.vertical ? dividerEl.offsetHeight : dividerEl.offsetWidth;
    const total = (state.vertical ? rect.height : rect.width) - dividerSize;
    if (total > 0) applyRatio(dragStartRatio + (pos - dragStartPos) / total, { persist: false });
  });

  function endDividerDrag(event) {
    if (!dragging) return;
    dragging = false;
    dividerEl.classList.remove('dragging');
    if (event && dividerEl.hasPointerCapture?.(event.pointerId)) {
      dividerEl.releasePointerCapture(event.pointerId);
    }

    if (moved) {
      lastTapTime = 0;
      if (state.ratio <= MIN_RATIO + EDGE_SNAP - 0.03) maximizePane(2);
      else if (state.ratio >= MAX_RATIO - EDGE_SNAP + 0.03) maximizePane(1);
      else saveSession();
      return;
    }

    // No movement: a tap. Two taps in quick succession swap the panes.
    const now = Date.now();
    if (now - lastTapTime < DOUBLE_TAP_MS) {
      lastTapTime = 0;
      swapPanes();
    } else {
      lastTapTime = now;
      setTimeout(() => {
        if (lastTapTime && Date.now() - lastTapTime >= DOUBLE_TAP_MS - 20) {
          lastTapTime = 0;
          showSplitOptions();
        }
      }, DOUBLE_TAP_MS);
    }
  }

  dividerEl.addEventListener('pointerup', endDividerDrag);
  dividerEl.addEventListener('pointercancel', endDividerDrag);
  dividerEl.addEventListener('keydown', (event) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      showSplitOptions();
    } else if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') {
      applyRatio(state.ratio - 0.05);
    } else if (event.key === 'ArrowRight' || event.key === 'ArrowDown') {
      applyRatio(state.ratio + 0.05);
    }
  });

  /* --------------------------------------------------------------- toolbar */

  qsa('[data-ratio]').forEach((button) => {
    button.addEventListener('click', () => {
      applyRatio(Number(button.dataset.ratio));
      qsa('[data-ratio]').forEach((other) => other.classList.toggle('active', other === button));
    });
  });

  $('btn-swap').addEventListener('click', swapPanes);
  $('btn-rotate').addEventListener('click', () => {
    setDirection(!state.vertical);
    toast(state.vertical ? 'Panes stacked top and bottom' : 'Panes side by side');
  });
  $('btn-apps').addEventListener('click', () => openPicker());
  $('btn-save-pair').addEventListener('click', promptSavePair);
  $('btn-pairs').addEventListener('click', openPairs);
  $('btn-floating').addEventListener('click', () => openFloating(state.floating.url));
  $('btn-help').addEventListener('click', showShortcuts);

  /* ------------------------------------------------------------------ dock */

  const dock = $('dock');
  $('dock-toggle').addEventListener('click', () => dock.classList.toggle('collapsed'));

  const dockItems = $('dock-items');
  [...DEMO_PAGES, ...WEB_APPS.slice(0, 6)].forEach((app) => {
    const item = document.createElement('button');
    item.className = 'dock-item';
    item.type = 'button';
    item.title = app.name;
    item.textContent = app.icon;
    item.addEventListener('click', () => showPlacementMenu(app));
    dockItems.appendChild(item);
  });

  const dockDivider = document.createElement('div');
  dockDivider.className = 'dock-divider';
  dockItems.appendChild(dockDivider);

  [
    { icon: '⇄', title: 'Swap panes', run: swapPanes },
    { icon: '▦', title: 'Pick two apps', run: () => openPicker() }
  ].forEach((action) => {
    const item = document.createElement('button');
    item.className = 'dock-item';
    item.type = 'button';
    item.title = action.title;
    item.textContent = action.icon;
    item.addEventListener('click', action.run);
    dockItems.appendChild(item);
  });

  /* ---------------------------------------------------------------- modals */

  function openModal(id) { $(id).hidden = false; }
  function closeModal(id) { $(id).hidden = true; }

  qsa('[data-close-modal]').forEach((button) => {
    button.addEventListener('click', () => closeModal(button.dataset.closeModal));
  });

  qsa('.modal').forEach((modal) => {
    modal.addEventListener('pointerdown', (event) => {
      if (event.target === modal) modal.hidden = true;
    });
  });

  function showActions(title, actions) {
    $('action-title').textContent = title;
    const list = $('action-list');
    list.innerHTML = '';
    actions.forEach((action) => {
      const row = document.createElement('button');
      row.type = 'button';
      row.className = 'action-row';
      row.innerHTML = `<span class="ar-icon"></span><span class="ar-label"></span>`;
      row.querySelector('.ar-icon').textContent = action.icon;
      row.querySelector('.ar-label').textContent = action.label;
      row.addEventListener('click', () => {
        closeModal('action-modal');
        action.run();
      });
      list.appendChild(row);
    });
    openModal('action-modal');
  }

  function showSplitOptions() {
    showActions('Split options', [
      { icon: '⇄', label: 'Swap the two panes', run: swapPanes },
      { icon: '▤', label: 'Even split (50:50)', run: () => applyRatio(0.5) },
      { icon: '⬒', label: 'Make Pane 1 full screen', run: () => toggleMaximize(1) },
      { icon: '⬓', label: 'Make Pane 2 full screen', run: () => toggleMaximize(2) },
      { icon: '⟲', label: 'Rotate split direction', run: () => setDirection(!state.vertical) },
      { icon: '⧉', label: 'Send Pane 2 to a floating window', run: () => openFloating(panes[2].url) },
      { icon: '★', label: 'Save this pair', run: promptSavePair }
    ]);
  }

  function showPaneMenu(id) {
    const pane = panes[id];
    showActions(`Pane ${id}`, [
      {
        icon: '⬒',
        label: state.maximized === id ? 'Exit full screen' : 'Full screen this pane',
        run: () => toggleMaximize(id)
      },
      { icon: '⧉', label: 'Open in floating window', run: () => openFloating(pane.url) },
      {
        icon: '⇄',
        label: 'Send to the other pane',
        run: () => loadPane(id === 1 ? 2 : 1, pane.url)
      },
      {
        icon: pane.desktop ? '📱' : '🖥',
        label: pane.desktop ? 'Use the mobile site' : 'Use the desktop site',
        run: () => setDesktopMode(id, !pane.desktop)
      },
      {
        icon: '⧉',
        label: 'Copy link',
        run: () => {
          navigator.clipboard?.writeText(pane.url);
          toast('Link copied');
        }
      },
      { icon: '↗', label: 'Open in a new tab', run: () => window.open(pane.url, '_blank', 'noopener') }
    ]);
  }

  function showPlacementMenu(app) {
    showActions(`Where should ${app.name} go?`, [
      { icon: '◧', label: 'Open in Pane 1', run: () => loadPane(1, app.url) },
      { icon: '◨', label: 'Open in Pane 2', run: () => loadPane(2, app.url) },
      { icon: '⧉', label: 'Open in the floating window', run: () => openFloating(app.url) }
    ]);
  }

  function showShortcuts() {
    showActions('Keyboard shortcuts', [
      { icon: '1', label: 'Ctrl + 1 / 2 — focus a pane', run: () => {} },
      { icon: '⇄', label: 'Ctrl + E — swap the panes', run: swapPanes },
      { icon: '↻', label: 'Ctrl + R — reload the focused pane', run: () => {} },
      { icon: '⟲', label: 'Ctrl + D — rotate the split', run: () => setDirection(!state.vertical) },
      { icon: '⬒', label: 'Ctrl + M — full screen the focused pane', run: () => {} },
      { icon: '⎋', label: 'Escape — leave full screen or close a dialog', run: () => {} }
    ]);
  }

  /* ----------------------------------------------------------- app picker */

  let pickerStage = 1;
  let pickerFirst = null;

  function openPicker() {
    pickerStage = 1;
    pickerFirst = null;
    $('picker-search').value = '';
    $('picker-step').textContent = 'Step 1 of 2';
    $('picker-title').textContent = 'Choose the first app';
    $('picker-desc').textContent = 'Pick what goes in Pane 1.';
    $('picker-back').hidden = true;
    renderPicker();
    openModal('picker-modal');
  }

  function goToPickerStage2() {
    pickerStage = 2;
    $('picker-search').value = '';
    $('picker-step').textContent = 'Step 2 of 2';
    $('picker-title').textContent = 'Choose the second app';
    $('picker-desc').textContent = `${pickerFirst.name} goes in Pane 1. Now pick what fills Pane 2.`;
    $('picker-back').hidden = false;
    renderPicker();
  }

  $('picker-back').addEventListener('click', openPicker);
  $('picker-search').addEventListener('input', renderPicker);

  function renderPicker() {
    const query = $('picker-search').value.trim().toLowerCase();
    const grid = $('picker-grid');
    grid.innerHTML = '';

    const matches = ALL_APPS.filter(
      (app) => !query || app.name.toLowerCase().includes(query) || app.url.toLowerCase().includes(query)
    );

    if (matches.length === 0) {
      const empty = document.createElement('p');
      empty.className = 'empty-note';
      empty.textContent = 'Nothing matched that search.';
      grid.appendChild(empty);
      return;
    }

    matches.forEach((app) => {
      const card = document.createElement('button');
      card.type = 'button';
      card.className = 'app-card';
      card.innerHTML = '<span class="ac-icon"></span><span><span class="ac-name"></span><br><span class="ac-sub"></span></span>';
      card.querySelector('.ac-icon').textContent = app.icon;
      card.querySelector('.ac-name').textContent = app.name;
      card.querySelector('.ac-sub').textContent = isLocalPage(app.url) ? 'demo page' : hostOf(app.url);
      card.addEventListener('click', () => {
        if (pickerStage === 1) {
          pickerFirst = app;
          goToPickerStage2();
        } else {
          closeModal('picker-modal');
          loadPane(1, pickerFirst.url);
          loadPane(2, app.url);
          restorePanes();
          toast(`${pickerFirst.name} + ${app.name}`);
        }
      });
      grid.appendChild(card);
    });
  }

  /* ------------------------------------------------------------- app pairs */

  function getPairs() {
    return store.read(STORE.pairs, DEFAULT_PAIRS);
  }

  function setPairs(pairs) {
    store.write(STORE.pairs, pairs);
  }

  function promptSavePair() {
    const suggested = `${hostOf(panes[1].url)} + ${hostOf(panes[2].url)}`;
    const title = window.prompt('Name this app pair:', suggested);
    if (!title || !title.trim()) return;
    setPairs([
      { id: String(Date.now()), title: title.trim(), p1: panes[1].url, p2: panes[2].url },
      ...getPairs()
    ]);
    toast('Pair saved');
  }

  function openPairs() {
    renderPairs();
    openModal('pairs-modal');
  }

  function renderPairs() {
    const list = $('pairs-list');
    const pairs = getPairs();
    list.innerHTML = '';

    if (pairs.length === 0) {
      const empty = document.createElement('p');
      empty.className = 'empty-note';
      empty.textContent = 'No pairs saved yet — set up a split and tap ★ Save pair.';
      list.appendChild(empty);
      return;
    }

    pairs.forEach((pair) => {
      const row = document.createElement('div');
      row.className = 'pair-row';
      row.innerHTML =
        '<span>★</span><div class="pair-info"><h4></h4><p></p></div>' +
        '<button class="btn" data-act="launch">Launch</button>' +
        '<button class="btn-icon" data-act="delete" title="Delete">✕</button>';
      row.querySelector('h4').textContent = pair.title;
      row.querySelector('p').textContent = `${hostOf(pair.p1)} + ${hostOf(pair.p2)}`;

      const launch = () => {
        closeModal('pairs-modal');
        loadPane(1, pair.p1);
        loadPane(2, pair.p2);
        restorePanes();
        toast(pair.title);
      };

      row.addEventListener('click', launch);
      row.querySelector('[data-act="launch"]').addEventListener('click', (event) => {
        event.stopPropagation();
        launch();
      });
      row.querySelector('[data-act="delete"]').addEventListener('click', (event) => {
        event.stopPropagation();
        setPairs(getPairs().filter((p) => p.id !== pair.id));
        renderPairs();
        toast('Pair deleted');
      });

      list.appendChild(row);
    });
  }

  /* ------------------------------------------------------- floating window */

  const floatEl = $('floating');
  const floatFrame = $('float-frame');
  const floatUrl = $('float-url');
  const systemBg = $('system-bg');
  const ALPHA_STEPS = [1, 0.85, 0.65, 0.45];

  function openFloating(url) {
    state.floating.url = normalizeUrl(url);
    state.floating.minimized = false;
    floatEl.classList.remove('minimized');
    floatEl.hidden = false;
    systemBg.hidden = false;
    floatUrl.value = isLocalPage(state.floating.url)
      ? hostOf(state.floating.url)
      : prettyUrl(state.floating.url);
    $('float-title').textContent = hostOf(state.floating.url);

    if (isFrameBlocked(state.floating.url)) {
      floatFrame.removeAttribute('src');
      floatFrame.srcdoc =
        '<body style="font:13px system-ui;padding:20px;color:#666">' +
        'This site blocks embedding in a browser. In the Android app it loads in a real WebView.' +
        '</body>';
    } else {
      floatFrame.removeAttribute('srcdoc');
      floatFrame.src = state.floating.url;
    }
  }

  function closeFloating() {
    floatEl.hidden = true;
    systemBg.hidden = true;
    floatEl.classList.remove('minimized');
    state.floating.minimized = false;
  }

  $('float-close').addEventListener('click', closeFloating);
  $('btn-close-system').addEventListener('click', closeFloating);

  $('float-minimize').addEventListener('click', () => {
    state.floating.minimized = true;
    floatEl.classList.add('minimized');
  });

  $('float-header').addEventListener('click', (event) => {
    if (state.floating.minimized && !event.target.closest('.float-actions')) {
      state.floating.minimized = false;
      floatEl.classList.remove('minimized');
    }
  });

  $('float-opacity').addEventListener('click', () => {
    state.floating.alphaStep = (state.floating.alphaStep + 1) % ALPHA_STEPS.length;
    floatEl.style.opacity = String(ALPHA_STEPS[state.floating.alphaStep]);
  });

  $('float-go').addEventListener('click', () => openFloating(floatUrl.value));
  floatUrl.addEventListener('keydown', (event) => {
    if (event.key === 'Enter') openFloating(floatUrl.value);
  });
  $('float-reload').addEventListener('click', () => openFloating(state.floating.url));
  $('float-back').addEventListener('click', () => {
    try {
      floatFrame.contentWindow?.history.back();
    } catch (err) {
      /* cross-origin frames will not let us walk their history */
    }
  });

  // Drag the header to move the window; it snaps to the nearest screen edge.
  makeDraggable($('float-header'), (dx, dy, start) => {
    const screen = $('screen').getBoundingClientRect();
    const width = floatEl.offsetWidth;
    const height = floatEl.offsetHeight;
    floatEl.style.left = `${Math.min(Math.max(start.left + dx, 0), screen.width - width)}px`;
    floatEl.style.top = `${Math.min(Math.max(start.top + dy, 0), screen.height - height)}px`;
  }, () => {
    const screen = $('screen').getBoundingClientRect();
    const width = floatEl.offsetWidth;
    const centre = floatEl.offsetLeft + width / 2;
    floatEl.style.left = centre < screen.width / 2 ? '8px' : `${screen.width - width - 8}px`;
  });

  // Drag the corner to resize it.
  makeDraggable($('float-resize'), (dx, dy, start) => {
    floatEl.style.width = `${Math.max(240, start.width + dx)}px`;
    floatEl.style.height = `${Math.max(200, start.height + dy)}px`;
  });

  function makeDraggable(handle, onMove, onEnd) {
    let active = false;
    let startX = 0;
    let startY = 0;
    let start = {};

    handle.addEventListener('pointerdown', (event) => {
      if (state.floating.minimized && handle === $('float-resize')) return;
      // Never start a drag from the window's own buttons, or preventDefault
      // below would swallow their click.
      if (event.target.closest('.float-actions, button')) return;
      active = true;
      startX = event.clientX;
      startY = event.clientY;
      start = {
        left: floatEl.offsetLeft,
        top: floatEl.offsetTop,
        width: floatEl.offsetWidth,
        height: floatEl.offsetHeight
      };
      handle.setPointerCapture(event.pointerId);
      event.preventDefault();
    });

    handle.addEventListener('pointermove', (event) => {
      if (!active) return;
      onMove(event.clientX - startX, event.clientY - startY, start);
    });

    const finish = (event) => {
      if (!active) return;
      active = false;
      if (handle.hasPointerCapture?.(event.pointerId)) handle.releasePointerCapture(event.pointerId);
      if (onEnd) onEnd();
    };

    handle.addEventListener('pointerup', finish);
    handle.addEventListener('pointercancel', finish);
  }

  /* ------------------------------------------------------------- shortcuts */

  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') {
      const open = qsa('.modal').find((modal) => !modal.hidden);
      if (open) { open.hidden = true; return; }
      if (state.maximized !== 0) { restorePanes(); return; }
      if (!floatEl.hidden) closeFloating();
      return;
    }

    if (!event.ctrlKey && !event.metaKey) return;
    const key = event.key.toLowerCase();
    const handlers = {
      '1': () => setActivePane(1),
      '2': () => setActivePane(2),
      e: swapPanes,
      r: () => loadPane(state.active, panes[state.active].url, { pushHistory: false }),
      d: () => setDirection(!state.vertical),
      m: () => toggleMaximize(state.active)
    };
    if (handlers[key]) {
      event.preventDefault();
      handlers[key]();
    }
  });

  window.addEventListener('resize', rescaleDesktopFrames);

  /* ------------------------------------------------------------------ boot */

  const session = store.read(STORE.session, null);
  setDirection(session?.vertical !== false);
  applyRatio(session?.ratio ?? 0.5, { persist: false });
  loadPane(1, session?.p1 || 'pages/player.html');
  loadPane(2, session?.p2 || 'pages/notes.html');
  setDesktopMode(1, Boolean(session?.d1));
  setDesktopMode(2, Boolean(session?.d2));
  setActivePane(1);

  qsa('[data-ratio]').forEach((button) => {
    button.classList.toggle('active', Math.abs(Number(button.dataset.ratio) - state.ratio) < 0.02);
  });

  // Exposed for the automated tests in tests/.
  window.__splitpad = { state, panes, applyRatio, swapPanes, toggleMaximize, setDirection, loadPane, normalizeUrl, hostOf };
})();
