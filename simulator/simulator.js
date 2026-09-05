document.addEventListener('DOMContentLoaded', () => {
  // DOM Elements
  const splitContainer = document.getElementById('split-container');
  const pane1 = document.getElementById('pane1');
  const pane2 = document.getElementById('pane2');
  const divider = document.getElementById('divider-slider');
  const splitRatioTooltip = document.getElementById('split-ratio-tooltip');

  const pane1UrlInput = document.getElementById('pane1-url-input');
  const pane2UrlInput = document.getElementById('pane2-url-input');
  const pane1Iframe = document.getElementById('pane1-iframe');
  const pane2Iframe = document.getElementById('pane2-iframe');
  const pane1Wrapper = document.getElementById('pane1-wrapper');
  const pane2Wrapper = document.getElementById('pane2-wrapper');

  const btnSwapPanes = document.getElementById('btn-swap-panes');
  const btnToggleDirection = document.getElementById('btn-toggle-direction');
  const btnOpenNativePicker = document.getElementById('btn-open-native-picker');
  const btnToggleFloating = document.getElementById('btn-toggle-floating');
  const floatingOverlay = document.getElementById('floating-overlay-window');
  const systemBgApp = document.getElementById('system-bg-app');
  const btnCloseBgMode = document.getElementById('btn-close-bg-mode');

  const btnQuickPair = document.getElementById('btn-quick-pair');
  const btnOpenPairs = document.getElementById('btn-open-pairs');
  const pairsModal = document.getElementById('pairs-modal');
  const btnClosePairsModal = document.getElementById('btn-close-pairs-modal');
  const savedPairsList = document.getElementById('saved-pairs-list');

  const nativeAppsModal = document.getElementById('native-apps-modal');
  const btnCloseNativeModal = document.getElementById('btn-close-native-modal');
  const nativeAppsGrid = document.getElementById('native-apps-grid');

  const targetSelectorModal = document.getElementById('target-selector-modal');
  const btnCloseTargetModal = document.getElementById('btn-close-target-modal');
  const targetModalTitle = document.getElementById('target-modal-title');

  const edgeDock = document.getElementById('edge-dock');
  const dockToggleBtn = document.getElementById('dock-toggle-btn');
  const dockContent = edgeDock.querySelector('.dock-content');

  let pendingTargetApp = null;
  let activePaneId = 1;

  // --- 1. DRAGGABLE SPLIT DIVIDER ---
  let isDraggingDivider = false;

  divider.addEventListener('mousedown', () => {
    isDraggingDivider = true;
    divider.classList.add('dragging');
    document.body.style.userSelect = 'none';
  });

  window.addEventListener('mouseup', () => {
    if (isDraggingDivider) {
      isDraggingDivider = false;
      divider.classList.remove('dragging');
      document.body.style.userSelect = '';
      updateDesktopScaleAll();
    }
  });

  window.addEventListener('mousemove', (e) => {
    if (!isDraggingDivider) return;

    const containerRect = splitContainer.getBoundingClientRect();
    const isVertical = splitContainer.classList.contains('split-vertical');

    if (isVertical) {
      const offsetY = e.clientY - containerRect.top;
      let ratio = offsetY / containerRect.height;
      ratio = Math.max(0.15, Math.min(0.85, ratio));
      setRatio(ratio);
    } else {
      const offsetX = e.clientX - containerRect.left;
      let ratio = offsetX / containerRect.width;
      ratio = Math.max(0.15, Math.min(0.85, ratio));
      setRatio(ratio);
    }
  });

  function setRatio(ratio) {
    pane1.style.flex = ratio;
    pane2.style.flex = 1 - ratio;
    const p1Percent = Math.round(ratio * 100);
    const p2Percent = 100 - p1Percent;
    if (splitRatioTooltip) {
      splitRatioTooltip.textContent = `${p1Percent}% : ${p2Percent}%`;
    }
    updateDesktopScaleAll();
  }

  // Active Pane Selection Glow
  pane1.addEventListener('click', () => setActivePane(1));
  pane2.addEventListener('click', () => setActivePane(2));

  function setActivePane(paneId) {
    activePaneId = paneId;
    if (paneId === 1) {
      pane1.classList.add('active-pane');
      pane2.classList.remove('active-pane');
    } else {
      pane2.classList.add('active-pane');
      pane1.classList.remove('active-pane');
    }
  }

  // --- 2. QUICK RATIO PILLS & 1-TAP SWAP PANES ---
  document.getElementById('btn-ratio-3070').addEventListener('click', (e) => {
    setActiveRatioPill(e.target);
    setRatio(0.3);
  });
  document.getElementById('btn-ratio-5050').addEventListener('click', (e) => {
    setActiveRatioPill(e.target);
    setRatio(0.5);
  });
  document.getElementById('btn-ratio-7030').addEventListener('click', (e) => {
    setActiveRatioPill(e.target);
    setRatio(0.7);
  });

  function setActiveRatioPill(targetPill) {
    document.querySelectorAll('.ratio-pill').forEach(p => p.classList.remove('active'));
    targetPill.classList.add('active');
  }

  function swapPanes() {
    const src1 = pane1Iframe.src;
    const src2 = pane2Iframe.src;

    loadPane1(src2);
    loadPane2(src1);
  }

  btnSwapPanes.addEventListener('click', swapPanes);
  document.getElementById('dock-btn-swap').addEventListener('click', swapPanes);

  // --- 3. ROTATE SPLIT DIRECTION ---
  btnToggleDirection.addEventListener('click', () => {
    if (splitContainer.classList.contains('split-vertical')) {
      splitContainer.classList.remove('split-vertical');
      splitContainer.classList.add('split-horizontal');
      btnToggleDirection.querySelector('.btn-text').textContent = 'Vertical Split';
    } else {
      splitContainer.classList.remove('split-horizontal');
      splitContainer.classList.add('split-vertical');
      btnToggleDirection.querySelector('.btn-text').textContent = 'Rotate Split';
    }
    pane1.style.flex = pane1.style.flex || '1';
    pane2.style.flex = pane2.style.flex || '1';
    updateDesktopScaleAll();
  });

  // --- 4. PERSISTENT TABLET SIDE EDGE DOCK ---
  dockToggleBtn.addEventListener('click', () => {
    dockContent.classList.toggle('collapsed');
  });

  document.querySelectorAll('.dock-item[data-url]').forEach(item => {
    item.addEventListener('click', () => {
      const appName = item.getAttribute('data-app');
      const appUrl = item.getAttribute('data-url');
      promptTargetPlacement(appName, appUrl);
    });
  });

  // --- 5. TARGET PLACEMENT ACTION MENU MODAL ---
  function promptTargetPlacement(appName, appUrl) {
    pendingTargetApp = { name: appName, url: appUrl };
    targetModalTitle.textContent = `Where to place ${appName}?`;
    targetSelectorModal.classList.remove('hidden');
  }

  document.getElementById('target-opt-pane1').addEventListener('click', () => {
    if (pendingTargetApp) loadPane1(pendingTargetApp.url);
    targetSelectorModal.classList.add('hidden');
  });

  document.getElementById('target-opt-pane2').addEventListener('click', () => {
    if (pendingTargetApp) loadPane2(pendingTargetApp.url);
    targetSelectorModal.classList.add('hidden');
  });

  document.getElementById('target-opt-floating').addEventListener('click', () => {
    if (pendingTargetApp) {
      document.getElementById('floating-url-input').value = pendingTargetApp.url;
      document.getElementById('floating-iframe').src = formatUrl(pendingTargetApp.url);
      systemBgApp.classList.remove('hidden');
      floatingOverlay.classList.remove('hidden');
    }
    targetSelectorModal.classList.add('hidden');
  });

  document.getElementById('target-opt-system').addEventListener('click', () => {
    if (pendingTargetApp) {
      alert(`Simulating Android System Multi-Window split for ${pendingTargetApp.name}! Native app launched in adjacent screen.`);
    }
    targetSelectorModal.classList.add('hidden');
  });

  btnCloseTargetModal.addEventListener('click', () => {
    targetSelectorModal.classList.add('hidden');
  });

  // --- 6. URL NAVIGATION & PRESETS ---
  function formatUrl(url) {
    let trimmed = url.trim();
    if (!trimmed) return 'https://www.google.com';
    if (!trimmed.startsWith('http://') && !trimmed.startsWith('https://')) {
      if (trimmed.includes('.') && !trimmed.includes(' ')) {
        return 'https://' + trimmed;
      }
      return 'https://www.google.com/search?q=' + encodeURIComponent(trimmed);
    }
    return trimmed;
  }

  function loadPane1(url) {
    const formatted = formatUrl(url);
    pane1UrlInput.value = formatted.replace('https://', '').replace('http://', '');
    pane1Iframe.src = formatted;
    setActivePane(1);
    updateDesktopScale(pane1, pane1Wrapper, pane1Iframe, !pane1UaBtn.classList.contains('active'));
  }

  function loadPane2(url) {
    const formatted = formatUrl(url);
    pane2UrlInput.value = formatted.replace('https://', '').replace('http://', '');
    pane2Iframe.src = formatted;
    setActivePane(2);
    updateDesktopScale(pane2, pane2Wrapper, pane2Iframe, !pane2UaBtn.classList.contains('active'));
  }

  document.getElementById('pane1-go').addEventListener('click', () => loadPane1(pane1UrlInput.value));
  document.getElementById('pane2-go').addEventListener('click', () => loadPane2(pane2UrlInput.value));

  pane1UrlInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') loadPane1(pane1UrlInput.value);
  });
  pane2UrlInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') loadPane2(pane2UrlInput.value);
  });

  document.getElementById('pane1-preset').addEventListener('change', (e) => loadPane1(e.target.value));
  document.getElementById('pane2-preset').addEventListener('change', (e) => loadPane2(e.target.value));

  document.getElementById('pane1-reload').addEventListener('click', () => pane1Iframe.src = pane1Iframe.src);
  document.getElementById('pane2-reload').addEventListener('click', () => pane2Iframe.src = pane2Iframe.src);

  // --- 7. USER-AGENT DESKTOP / MOBILE TOGGLE & VIEWPORT SCALE ---
  const pane1UaBtn = document.getElementById('pane1-ua-btn');
  const pane2UaBtn = document.getElementById('pane2-ua-btn');

  function updateDesktopScale(paneBox, iframeWrapper, iframe, isDesktop) {
    if (isDesktop) {
      paneBox.classList.add('desktop-scaled');
      const rect = iframeWrapper.getBoundingClientRect();
      if (rect.width > 0) {
        const scale = rect.width / 1280;
        iframe.style.transform = `scale(${scale})`;
      }
    } else {
      paneBox.classList.remove('desktop-scaled');
      iframe.style.transform = '';
    }
  }

  function updateDesktopScaleAll() {
    updateDesktopScale(pane1, pane1Wrapper, pane1Iframe, !pane1UaBtn.classList.contains('active'));
    updateDesktopScale(pane2, pane2Wrapper, pane2Iframe, !pane2UaBtn.classList.contains('active'));
  }

  pane1UaBtn.addEventListener('click', () => {
    const isMobile = pane1UaBtn.classList.toggle('active');
    pane1UaBtn.textContent = isMobile ? '📱 Mobile' : '🖥️ Desktop';
    updateDesktopScale(pane1, pane1Wrapper, pane1Iframe, !isMobile);
  });

  pane2UaBtn.addEventListener('click', () => {
    const isMobile = pane2UaBtn.classList.toggle('active');
    pane2UaBtn.textContent = isMobile ? '📱 Mobile' : '🖥️ Desktop';
    updateDesktopScale(pane2, pane2Wrapper, pane2Iframe, !isMobile);
  });

  window.addEventListener('resize', updateDesktopScaleAll);

  // --- 8. 2-STAGE STEP-BY-STEP APP PICKER & SMART LAUNCH ---
  const sampleNativeApps = [
    { id: 'zalo', name: 'Zalo Native App', icon: '💬', class: 'icon-blue', url: 'https://zalo.me', isNative: true },
    { id: 'chrome', name: 'Google Chrome', icon: '🌐', class: 'icon-cyan', url: 'https://www.google.com', isNative: false },
    { id: 'youtube', name: 'YouTube', icon: '▶️', class: 'icon-red', url: 'https://m.youtube.com', isNative: false },
    { id: 'facebook', name: 'Facebook', icon: '👤', class: 'icon-blue', url: 'https://m.facebook.com', isNative: false },
    { id: 'chatgpt', name: 'ChatGPT AI', icon: '🤖', class: 'icon-green', url: 'https://chatgpt.com', isNative: false },
    { id: 'wikipedia', name: 'Wikipedia', icon: '📚', class: 'icon-purple', url: 'https://www.wikipedia.org', isNative: false },
    { id: 'files', name: 'Files & Storage', icon: '📁', class: 'icon-orange', url: 'https://docs.google.com', isNative: true },
    { id: 'reddit', name: 'Reddit', icon: '🔴', class: 'icon-red', url: 'https://www.reddit.com', isNative: false },
    { id: 'news', name: 'Google News', icon: '📰', class: 'icon-blue', url: 'https://news.google.com', isNative: false },
    { id: 'tiktok', name: 'TikTok', icon: '🎵', class: 'icon-dark', url: 'https://www.tiktok.com', isNative: false }
  ];

  let pickerStage = 1;
  let selectedApp1 = null;
  let selectedApp2 = null;

  const pickerStageBadge = document.getElementById('picker-stage-badge');
  const pickerStageTitle = document.getElementById('picker-stage-title');
  const pickerStageDesc = document.getElementById('picker-stage-desc');
  const appSearchInput = document.getElementById('app-search-input');

  function openStage1Picker() {
    pickerStage = 1;
    selectedApp1 = null;
    selectedApp2 = null;
    if (appSearchInput) appSearchInput.value = '';
    
    if (pickerStageBadge) pickerStageBadge.textContent = 'Step 1 of 2';
    if (pickerStageTitle) pickerStageTitle.textContent = 'Select App 1 (Main / Native App)';
    if (pickerStageDesc) pickerStageDesc.textContent = 'Tap App 1 (e.g. Zalo) to select it, then automatically proceed to select App 2:';
    
    renderNativeApps();
    nativeAppsModal.classList.remove('hidden');
  }

  function openStage2Picker() {
    pickerStage = 2;
    if (appSearchInput) appSearchInput.value = '';

    if (pickerStageBadge) pickerStageBadge.textContent = 'Step 2 of 2';
    if (pickerStageTitle) pickerStageTitle.textContent = `Select App 2 (Overlay over ${selectedApp1.name})`;
    if (pickerStageDesc) pickerStageDesc.textContent = `Selected App 1: ${selectedApp1.name}. Now tap App 2 (e.g. Chrome) to finish & split!`;

    renderNativeApps();
  }

  function renderNativeApps() {
    nativeAppsGrid.innerHTML = '';
    const query = appSearchInput ? appSearchInput.value.toLowerCase().trim() : '';

    const filtered = sampleNativeApps.filter(app => 
      !query || app.name.toLowerCase().includes(query)
    );

    if (filtered.length === 0) {
      nativeAppsGrid.innerHTML = '<p style="grid-column: 1/-1; text-align: center; color: var(--text-secondary); padding: 20px;">No matching apps found</p>';
      return;
    }

    filtered.forEach(app => {
      const card = document.createElement('div');
      card.className = 'native-app-card';
      card.innerHTML = `
        <div class="icon-box ${app.class}">${app.icon}</div>
        <span>${app.name}</span>
      `;
      card.addEventListener('click', () => {
        if (pickerStage === 1) {
          selectedApp1 = app;
          openStage2Picker();
        } else {
          selectedApp2 = app;
          nativeAppsModal.classList.add('hidden');
          executeSmartLaunch(selectedApp1, selectedApp2);
        }
      });
      nativeAppsGrid.appendChild(card);
    });
  }

  if (appSearchInput) {
    appSearchInput.addEventListener('input', renderNativeApps);
  }

  function executeSmartLaunch(app1, app2) {
    // Load App 1 into Pane 1 and App 2 into Pane 2 inside Dual Split View
    loadPane1(app1.url);
    loadPane2(app2.url);
    systemBgApp.classList.add('hidden');

    alert(`⚡ 2-Stage Dual App Split Loaded!\n\nPane 1 (Left / Top): ${app1.name}\nPane 2 (Right / Bottom): ${app2.name}`);
  }

  btnOpenNativePicker.addEventListener('click', openStage1Picker);

  const dockBtnNative = document.getElementById('dock-btn-native');
  if (dockBtnNative) dockBtnNative.addEventListener('click', openStage1Picker);

  btnCloseNativeModal.addEventListener('click', () => {
    nativeAppsModal.classList.add('hidden');
  });

  // --- 9. FLOATING OVERLAY WINDOW ---
  btnToggleFloating.addEventListener('click', () => {
    systemBgApp.classList.remove('hidden');
    floatingOverlay.classList.remove('hidden');
  });

  btnCloseBgMode.addEventListener('click', () => {
    systemBgApp.classList.add('hidden');
    floatingOverlay.classList.add('hidden');
  });

  document.getElementById('btn-close-floating').addEventListener('click', () => {
    floatingOverlay.classList.add('hidden');
    systemBgApp.classList.add('hidden');
  });

  const btnMinimizeFloating = document.getElementById('btn-minimize-floating');
  if (btnMinimizeFloating) {
    btnMinimizeFloating.addEventListener('click', () => {
      const body = floatingOverlay.querySelector('.floating-body');
      const toolbar = floatingOverlay.querySelector('.floating-toolbar');
      if (body.style.display === 'none') {
        body.style.display = 'block';
        toolbar.style.display = 'flex';
        floatingOverlay.style.height = '540px';
      } else {
        body.style.display = 'none';
        toolbar.style.display = 'none';
        floatingOverlay.style.height = '40px';
      }
    });
  }

  const floatingHeader = document.getElementById('floating-drag-handle');
  let isDraggingFloating = false;
  let floatStartX, floatStartY, floatInitialLeft, floatInitialTop;

  floatingHeader.addEventListener('mousedown', (e) => {
    isDraggingFloating = true;
    floatStartX = e.clientX;
    floatStartY = e.clientY;
    const rect = floatingOverlay.getBoundingClientRect();
    const parentRect = document.getElementById('tablet-screen').getBoundingClientRect();
    floatInitialLeft = rect.left - parentRect.left;
    floatInitialTop = rect.top - parentRect.top;
  });

  window.addEventListener('mousemove', (e) => {
    if (!isDraggingFloating) return;
    const dx = e.clientX - floatStartX;
    const dy = e.clientY - floatStartY;
    floatingOverlay.style.left = `${floatInitialLeft + dx}px`;
    floatingOverlay.style.top = `${floatInitialTop + dy}px`;
  });

  window.addEventListener('mouseup', () => {
    isDraggingFloating = false;
  });

  const floatingUrlInput = document.getElementById('floating-url-input');
  const floatingIframe = document.getElementById('floating-iframe');
  document.getElementById('floating-go-btn').addEventListener('click', () => {
    floatingIframe.src = formatUrl(floatingUrlInput.value);
  });

  // --- 10. SAVED APP PAIRS MANAGER ---
  const defaultPairs = [
    { id: '1', title: 'YouTube + Google Search', p1: 'https://m.youtube.com', p2: 'https://www.google.com' },
    { id: '2', title: 'ChatGPT + Wikipedia', p1: 'https://chatgpt.com', p2: 'https://www.wikipedia.org' },
    { id: '3', title: 'Reddit + Google News', p1: 'https://www.reddit.com', p2: 'https://news.google.com' }
  ];

  function getStoredPairs() {
    const raw = localStorage.getItem('redmi_app_pairs');
    return raw ? JSON.parse(raw) : defaultPairs;
  }

  function renderSavedPairs() {
    const pairs = getStoredPairs();
    savedPairsList.innerHTML = '';

    if (pairs.length === 0) {
      savedPairsList.innerHTML = '<p class="modal-desc">No saved app pairs yet.</p>';
      return;
    }

    pairs.forEach(pair => {
      const item = document.createElement('div');
      item.className = 'saved-pair-item';
      item.innerHTML = `
        <div class="pair-info">
          <h4>⭐ ${pair.title}</h4>
          <p>${pair.p1}  |  ${pair.p2}</p>
        </div>
        <div class="pair-actions" style="display:flex; gap:6px;">
          <button class="btn btn-secondary btn-sm btn-launch-pair">Launch</button>
          <button class="btn btn-secondary btn-sm btn-delete-pair" style="color:#FF3B30;">Delete</button>
        </div>
      `;

      item.querySelector('.btn-launch-pair').addEventListener('click', (e) => {
        e.stopPropagation();
        loadPane1(pair.p1);
        loadPane2(pair.p2);
        pairsModal.classList.add('hidden');
      });

      item.querySelector('.btn-delete-pair').addEventListener('click', (e) => {
        e.stopPropagation();
        const current = getStoredPairs().filter(p => p.id !== pair.id);
        localStorage.setItem('redmi_app_pairs', JSON.stringify(current));
        renderSavedPairs();
      });

      item.addEventListener('click', () => {
        loadPane1(pair.p1);
        loadPane2(pair.p2);
        pairsModal.classList.add('hidden');
      });

      savedPairsList.appendChild(item);
    });
  }

  btnQuickPair.addEventListener('click', () => {
    const title = prompt('Enter a name for this app pair:', 'Custom Pair');
    if (title && title.trim()) {
      const pairs = getStoredPairs();
      pairs.unshift({
        id: Date.now().toString(),
        title: title.trim(),
        p1: pane1Iframe.src,
        p2: pane2Iframe.src
      });
      localStorage.setItem('redmi_app_pairs', JSON.stringify(pairs));
      alert('App Pair Saved!');
    }
  });

  btnOpenPairs.addEventListener('click', () => {
    renderSavedPairs();
    pairsModal.classList.remove('hidden');
  });

  btnClosePairsModal.addEventListener('click', () => {
    pairsModal.classList.add('hidden');
  });
});
