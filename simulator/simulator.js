/* =============================================================================
   Split Pad simulator

   Mirrors the Android app: two- or three-pane layouts, ratio presets, draggable
   dividers with drag-to-edge full screen, rotate, per-pane navigation and
   desktop mode, a panel menu that moves panes between slots, saved app pairs
   and a two-step app picker.

   Layouts
     2 panes   pane 1 | pane 2
     3 panes   pane 1 | (pane 2 over pane 3)

   Everything uses Pointer Events, so the same code path serves mouse, touch and
   pen. Menus open on pointer-up with no double-tap window to wait out.
   ============================================================================= */
(() => {
  'use strict';

  const MIN_RATIO = 0.15;
  const MAX_RATIO = 0.85;
  const EDGE_SNAP = 0.03;
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
    toastTimer = setTimeout(() => el.classList.remove('show'), 2000);
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
    if (mode === 'auto') document.documentElement.removeAttribute('data-theme');
    else document.documentElement.setAttribute('data-theme', mode);
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
  const secondaryGroup = $('secondary-group');
  const dividers = { primary: $('divider'), secondary: $('divider-2') };
  const tips = { primary: $('ratio-tip'), secondary: $('ratio-tip-2') };

  const panes = { 1: makePane(1), 2: makePane(2), 3: makePane(3) };

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
      loaded: false,
      history: [],
      historyIndex: -1
    };
  }

  const state = {
    ratio: 0.5,
    secondaryRatio: 0.5,
    vertical: true,
    paneCount: 2,
    maximized: 0,
    active: 1
  };

  function saveSession() {
    store.write(STORE.session, {
      urls: [panes[1].url, panes[2].url, panes[3].url],
      desktop: [panes[1].desktop, panes[2].desktop, panes[3].desktop],
      ratio: state.ratio,
      secondaryRatio: state.secondaryRatio,
      vertical: state.vertical,
      paneCount: state.paneCount
    });
  }

  function loadPane(id, rawUrl, { pushHistory = true } = {}) {
    const pane = panes[id];
    const url = normalizeUrl(rawUrl);
    pane.url = url;
    pane.loaded = true;
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
    }, 240);
  }

  [1, 2, 3].forEach((id) => {
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
      window.open(panes[Number(el.dataset.openTab)].url, '_blank', 'noopener');
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
    [1, 2, 3].forEach((n) => panes[n].root.classList.toggle('active', n === id));
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
      pane.frame.style.transform = `scale(${Math.max(rect.width / 1280, 0.2)})`;
    } else {
      pane.frame.style.transform = '';
    }
    saveSession();
  }

  let rescaleQueued = false;
  function rescaleDesktopFrames() {
    // Coalesced into one frame: resizing fired this dozens of times a second.
    if (rescaleQueued) return;
    rescaleQueued = true;
    requestAnimationFrame(() => {
      rescaleQueued = false;
      [1, 2, 3].forEach((id) => { if (panes[id].desktop) setDesktopMode(id, true); });
    });
  }

  /* ------------------------------------------------------------------ split */

  function applyLayout() {
    const three = state.paneCount === 3;
    const max = state.maximized;

    panes[1].root.classList.toggle('hidden-pane', max !== 0 && max !== 1);
    panes[2].root.classList.toggle('hidden-pane', max !== 0 && max !== 2);
    panes[3].root.classList.toggle('hidden-pane', !three || (max !== 0 && max !== 3));

    secondaryGroup.classList.toggle('hidden-pane', max === 1);
    dividers.secondary.classList.toggle('hidden-pane', !three);

    if (max === 1) {
      panes[1].root.style.flex = '1 1 0%';
    } else if (max === 2 || max === 3) {
      secondaryGroup.style.flex = '1 1 0%';
      panes[max].root.style.flex = '1 1 0%';
    } else {
      panes[1].root.style.flex = `${state.ratio} 1 0%`;
      secondaryGroup.style.flex = `${1 - state.ratio} 1 0%`;
      panes[2].root.style.flex = three ? `${state.secondaryRatio} 1 0%` : '1 1 0%';
      panes[3].root.style.flex = three ? `${1 - state.secondaryRatio} 1 0%` : '0 1 0%';
    }

    const left = Math.round(state.ratio * 100);
    tips.primary.textContent = `${left} : ${100 - left}`;
    const upper = Math.round(state.secondaryRatio * 100);
    tips.secondary.textContent = `${upper} : ${100 - upper}`;

    rescaleDesktopFrames();
  }

  function applyRatio(ratio, { persist = true } = {}) {
    if (state.maximized !== 0) restorePanes();
    state.ratio = Math.min(MAX_RATIO, Math.max(MIN_RATIO, ratio));
    applyLayout();
    if (persist) saveSession();
  }

  function applySecondaryRatio(ratio, { persist = true } = {}) {
    if (state.maximized !== 0) restorePanes();
    state.secondaryRatio = Math.min(MAX_RATIO, Math.max(MIN_RATIO, ratio));
    applyLayout();
    if (persist) saveSession();
  }

  function maximizePane(id) {
    if (state.ratio <= MIN_RATIO + 0.02 || state.ratio >= MAX_RATIO - 0.02) state.ratio = 0.5;
    if (state.secondaryRatio <= MIN_RATIO + 0.02 || state.secondaryRatio >= MAX_RATIO - 0.02) {
      state.secondaryRatio = 0.5;
    }
    state.maximized = id;
    applyLayout();
    setActivePane(id);
    saveSession();
    toast('Full screen — drag the divider or press Escape to return');
  }

  function restorePanes() {
    if (state.maximized === 0) return;
    state.maximized = 0;
    applyLayout();
  }

  function toggleMaximize(id) {
    if (state.maximized === id) restorePanes(); else maximizePane(id);
  }

  function setDirection(vertical) {
    state.vertical = vertical;
    splitEl.classList.toggle('vertical', vertical);
    splitEl.classList.toggle('horizontal', !vertical);
    applyLayout();
    saveSession();
  }

  function setPaneCount(count) {
    if (count === 3 && state.paneCount !== 3) {
      // Three panes read as "one full half, plus two stacked", which needs the
      // primary split to run left/right.
      setDirection(false);
    }
    state.paneCount = count;
    if (state.maximized > count) state.maximized = 0;
    if (count === 3 && !panes[3].loaded) loadPane(3, 'pages/reader.html');
    applyLayout();
    updateLayoutButtons();
    saveSession();
    toast(count === 3 ? 'Three panes: one half, plus two stacked' : 'Two panes');
  }

  function updateLayoutButtons() {
    $('btn-layout-2').classList.toggle('active', state.paneCount === 2);
    $('btn-layout-3').classList.toggle('active', state.paneCount === 3);
  }

  /** Exchanges what two panes are showing, keeping each pane's own chrome. */
  function swapPanes(a, b) {
    if (a === b || a > state.paneCount || b > state.paneCount) return;
    const urlA = panes[a].url;
    const urlB = panes[b].url;
    const deskA = panes[a].desktop;
    const deskB = panes[b].desktop;
    loadPane(a, urlB);
    loadPane(b, urlA);
    setDesktopMode(a, deskB);
    setDesktopMode(b, deskA);
  }

  /** Human name for a slot, which depends on the current layout. */
  function positionName(id) {
    if (state.paneCount === 2) {
      if (state.vertical) return id === 1 ? 'Top' : 'Bottom';
      return id === 1 ? 'Left' : 'Right';
    }
    if (state.vertical) return ['Top half', 'Bottom left', 'Bottom right'][id - 1];
    return ['Left half', 'Top right', 'Bottom right'][id - 1];
  }

  function closePane(id) {
    if (state.paneCount !== 3) return;
    // Shuffle the survivors up so the remaining two keep the expected slots.
    if (id === 1) { swapPanes(1, 2); swapPanes(2, 3); } else if (id === 2) { swapPanes(2, 3); }
    setPaneCount(2);
    setActivePane(id === 1 ? 1 : Math.min(id, 2));
  }

  /* --------------------------------------------------- divider interaction */

  function wireDivider(divider, which) {
    let dragging = false;
    let startPos = 0;
    let startRatio = 0.5;
    let moved = false;

    const isPrimary = which === 'primary';
    // The primary divider follows the main axis; the secondary runs across it.
    const alongVertical = () => (isPrimary ? state.vertical : !state.vertical);

    divider.addEventListener('pointerdown', (event) => {
      dragging = true;
      moved = false;
      startPos = alongVertical() ? event.clientY : event.clientX;
      startRatio = isPrimary ? state.ratio : state.secondaryRatio;
      divider.setPointerCapture(event.pointerId);
      divider.classList.add('dragging');
    });

    divider.addEventListener('pointermove', (event) => {
      if (!dragging) return;
      const pos = alongVertical() ? event.clientY : event.clientX;
      if (!moved && Math.abs(pos - startPos) > 4) {
        moved = true;
        if (state.maximized !== 0) {
          restorePanes();
          startRatio = isPrimary ? state.ratio : state.secondaryRatio;
          startPos = pos;
        }
      }
      if (!moved) return;
      const host = (isPrimary ? splitEl : secondaryGroup).getBoundingClientRect();
      const span = alongVertical() ? host.height : host.width;
      const thickness = alongVertical() ? divider.offsetHeight : divider.offsetWidth;
      const total = span - thickness;
      if (total <= 0) return;
      const next = startRatio + (pos - startPos) / total;
      if (isPrimary) applyRatio(next, { persist: false });
      else applySecondaryRatio(next, { persist: false });
    });

    function end(event) {
      if (!dragging) return;
      dragging = false;
      divider.classList.remove('dragging');
      if (event && divider.hasPointerCapture?.(event.pointerId)) {
        divider.releasePointerCapture(event.pointerId);
      }

      if (!moved) {
        // A tap. The sheet opens right here on pointer-up — there is no
        // double-tap window to wait out, so it feels immediate.
        showSplitOptions();
        return;
      }

      if (isPrimary) {
        if (state.ratio <= MIN_RATIO + EDGE_SNAP && state.paneCount === 2) maximizePane(2);
        else if (state.ratio >= MAX_RATIO - EDGE_SNAP) maximizePane(1);
        else saveSession();
      } else if (state.secondaryRatio <= MIN_RATIO + EDGE_SNAP) {
        maximizePane(3);
      } else if (state.secondaryRatio >= MAX_RATIO - EDGE_SNAP) {
        maximizePane(2);
      } else {
        saveSession();
      }
    }

    divider.addEventListener('pointerup', end);
    divider.addEventListener('pointercancel', end);

    divider.addEventListener('keydown', (event) => {
      const step = isPrimary ? applyRatio : applySecondaryRatio;
      const current = isPrimary ? state.ratio : state.secondaryRatio;
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        showSplitOptions();
      } else if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') {
        step(current - 0.05);
      } else if (event.key === 'ArrowRight' || event.key === 'ArrowDown') {
        step(current + 0.05);
      }
    });
  }

  wireDivider(dividers.primary, 'primary');
  wireDivider(dividers.secondary, 'secondary');

  /* --------------------------------------------------------------- toolbar */

  qsa('[data-ratio]').forEach((button) => {
    button.addEventListener('click', () => {
      applyRatio(Number(button.dataset.ratio));
      qsa('[data-ratio]').forEach((other) => other.classList.toggle('active', other === button));
    });
  });

  $('btn-swap').addEventListener('click', () => {
    swapPanes(1, 2);
    toast('Panes swapped');
  });
  $('btn-rotate').addEventListener('click', () => {
    setDirection(!state.vertical);
    toast(state.vertical ? 'Panes stacked top and bottom' : 'Panes side by side');
  });
  $('btn-apps').addEventListener('click', () => openPicker());
  $('btn-save-pair').addEventListener('click', promptSavePair);
  $('btn-pairs').addEventListener('click', openPairs);
  $('btn-help').addEventListener('click', showShortcuts);
  $('btn-layout-2').addEventListener('click', () => setPaneCount(2));
  $('btn-layout-3').addEventListener('click', () => setPaneCount(3));

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
    { icon: '⇄', title: 'Swap panes', run: () => { swapPanes(1, 2); toast('Panes swapped'); } },
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

  const actionList = $('action-list');

  function showActions(title, actions) {
    $('action-title').textContent = title;
    // One fragment, one reflow, rather than appending row by row.
    const fragment = document.createDocumentFragment();
    actions.forEach((action) => {
      const row = document.createElement('button');
      row.type = 'button';
      row.className = 'action-row';
      row.innerHTML = '<span class="ar-icon"></span><span class="ar-label"></span>';
      row.querySelector('.ar-icon').textContent = action.icon;
      row.querySelector('.ar-label').textContent = action.label;
      row.addEventListener('click', () => {
        closeModal('action-modal');
        action.run();
      });
      fragment.appendChild(row);
    });
    actionList.replaceChildren(fragment);
    openModal('action-modal');
  }

  function showSplitOptions() {
    const actions = [
      { icon: '⇄', label: 'Swap the two panes', run: () => { swapPanes(1, 2); toast('Panes swapped'); } },
      {
        icon: '▤',
        label: 'Even split',
        run: () => { applyRatio(0.5); applySecondaryRatio(0.5); }
      },
      { icon: '⬒', label: 'Full screen the focused pane', run: () => toggleMaximize(state.active) },
      { icon: '⟲', label: 'Rotate split direction', run: () => setDirection(!state.vertical) },
      state.paneCount === 2
        ? { icon: '▥', label: 'Use three panes (one half + two stacked)', run: () => setPaneCount(3) }
        : { icon: '▤', label: 'Use two panes', run: () => setPaneCount(2) },
      { icon: '★', label: 'Save this pair', run: promptSavePair }
    ];
    showActions('Split options', actions);
  }

  function showPaneMenu(id) {
    const pane = panes[id];
    const actions = [
      {
        icon: '⬒',
        label: state.maximized === id ? 'Exit full screen' : 'Full screen this pane',
        run: () => toggleMaximize(id)
      }
    ];

    for (let target = 1; target <= state.paneCount; target += 1) {
      if (target === id) continue;
      actions.push({
        icon: '⇄',
        label: `Move to ${positionName(target)}`,
        run: () => {
          swapPanes(id, target);
          setActivePane(target);
          toast(`Moved to ${positionName(target)}`);
        }
      });
    }

    actions.push(
      state.paneCount === 2
        ? { icon: '▥', label: 'Add a third pane', run: () => setPaneCount(3) }
        : { icon: '✕', label: 'Close this pane', run: () => closePane(id) }
    );

    actions.push(
      {
        icon: pane.desktop ? '📱' : '🖥',
        label: pane.desktop ? 'Use the mobile site' : 'Use the desktop site',
        run: () => setDesktopMode(id, !pane.desktop)
      },
      {
        icon: '⧉',
        label: 'Copy link',
        run: () => { navigator.clipboard?.writeText(pane.url); toast('Link copied'); }
      },
      { icon: '↗', label: 'Open in a new tab', run: () => window.open(pane.url, '_blank', 'noopener') }
    );

    showActions(`Pane ${id} · ${positionName(id)}`, actions);
  }

  function showPlacementMenu(app) {
    const actions = [];
    for (let id = 1; id <= state.paneCount; id += 1) {
      const target = id;
      actions.push({
        icon: '◧',
        label: `Open in ${positionName(target)}`,
        run: () => loadPane(target, app.url)
      });
    }
    showActions(`Where should ${app.name} go?`, actions);
  }

  function showShortcuts() {
    showActions('Keyboard shortcuts', [
      { icon: '1', label: 'Ctrl + 1 / 2 / 3 — focus a pane', run: () => {} },
      { icon: '⇄', label: 'Ctrl + E — swap panes 1 and 2', run: () => swapPanes(1, 2) },
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
    $('picker-desc').textContent = `Pick what goes in ${positionName(1)}.`;
    $('picker-back').hidden = true;
    renderPicker();
    openModal('picker-modal');
  }

  function goToPickerStage2() {
    pickerStage = 2;
    $('picker-search').value = '';
    $('picker-step').textContent = 'Step 2 of 2';
    $('picker-title').textContent = 'Choose the second app';
    $('picker-desc').textContent =
      `${pickerFirst.name} goes in ${positionName(1)}. Now pick what fills ${positionName(2)}.`;
    $('picker-back').hidden = false;
    renderPicker();
  }

  $('picker-back').addEventListener('click', openPicker);
  $('picker-search').addEventListener('input', renderPicker);

  const pickerGrid = $('picker-grid');

  function renderPicker() {
    const query = $('picker-search').value.trim().toLowerCase();
    const matches = ALL_APPS.filter(
      (app) => !query || app.name.toLowerCase().includes(query) || app.url.toLowerCase().includes(query)
    );

    if (matches.length === 0) {
      const empty = document.createElement('p');
      empty.className = 'empty-note';
      empty.textContent = 'Nothing matched that search.';
      pickerGrid.replaceChildren(empty);
      return;
    }

    const fragment = document.createDocumentFragment();
    matches.forEach((app) => {
      const card = document.createElement('button');
      card.type = 'button';
      card.className = 'app-card';
      card.innerHTML =
        '<span class="ac-icon"></span><span><span class="ac-name"></span><br><span class="ac-sub"></span></span>';
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
      fragment.appendChild(card);
    });
    pickerGrid.replaceChildren(fragment);
  }

  /* ------------------------------------------------------------- app pairs */

  const getPairs = () => store.read(STORE.pairs, DEFAULT_PAIRS);
  const setPairs = (pairs) => store.write(STORE.pairs, pairs);

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

  const pairsList = $('pairs-list');

  function renderPairs() {
    const pairs = getPairs();
    if (pairs.length === 0) {
      const empty = document.createElement('p');
      empty.className = 'empty-note';
      empty.textContent = 'No pairs saved yet — set up a split and tap ★ Save pair.';
      pairsList.replaceChildren(empty);
      return;
    }

    const fragment = document.createDocumentFragment();
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

      fragment.appendChild(row);
    });
    pairsList.replaceChildren(fragment);
  }

  /* ------------------------------------------------------------- shortcuts */

  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') {
      const open = qsa('.modal').find((modal) => !modal.hidden);
      if (open) { open.hidden = true; return; }
      if (state.maximized !== 0) restorePanes();
      return;
    }

    if (!event.ctrlKey && !event.metaKey) return;
    const key = event.key.toLowerCase();
    const handlers = {
      '1': () => setActivePane(1),
      '2': () => setActivePane(2),
      '3': () => { if (state.paneCount === 3) setActivePane(3); },
      e: () => { swapPanes(1, 2); toast('Panes swapped'); },
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
  state.ratio = session?.ratio ?? 0.5;
  state.secondaryRatio = session?.secondaryRatio ?? 0.5;
  state.paneCount = session?.paneCount === 3 ? 3 : 2;
  setDirection(session?.vertical !== false);

  const urls = session?.urls || ['pages/player.html', 'pages/notes.html', 'pages/reader.html'];
  loadPane(1, urls[0] || 'pages/player.html');
  loadPane(2, urls[1] || 'pages/notes.html');
  if (state.paneCount === 3) loadPane(3, urls[2] || 'pages/reader.html');

  (session?.desktop || []).forEach((desktop, index) => setDesktopMode(index + 1, Boolean(desktop)));
  applyLayout();
  updateLayoutButtons();
  setActivePane(1);

  qsa('[data-ratio]').forEach((button) => {
    button.classList.toggle('active', Math.abs(Number(button.dataset.ratio) - state.ratio) < 0.02);
  });

  // Exposed for the automated tests in tests/.
  window.__splitpad = {
    state, panes, applyRatio, applySecondaryRatio, swapPanes, toggleMaximize,
    setDirection, setPaneCount, loadPane, positionName, normalizeUrl, hostOf
  };
})();
