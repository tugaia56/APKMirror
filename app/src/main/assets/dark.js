// Dark theme for apkmirror.com (runs in the WebView only when the app is dark).
// Colours: the app fills the %NAMES% from its colour resources (page_bg, card_bg, card_bg_2, card_bg_3, header_bg, colorPrimary
// in res/values/colors.xml); the Oxydian Theme overlay replaces them.
(function () {
    if (window.__apkmDark) return;
    window.__apkmDark = true;

    var PAGE_BG = '%PAGE_BG%';    // page background (behind the cards)
    var CARD_BG = '%CARD_BG%';    // cards, lists, panels (lighter than the page)
    var CARD_BG_2 = '%CARD_BG_2%';  // slightly grey light areas (stripes, hover rows, inputs)
    var CARD_BG_3 = '%CARD_BG_3%';  // other light greys
    var HEADER_BG = '%HEADER_BG%';  // site header (search bar and breadcrumbs)
    var ACCENT = '%ACCENT%';        // accent: the orange of the site and the blue-greys of the texts
    var INFO = '%INFO%';            // circle of the (i) icons (the accent when they are active)
    var INFO_GLYPH = '#FFFFFF';     // the "i" inside the circle

    // Colours of the site (as in the page CSS) that become the accent
    var ACCENT_TEXT = {'rgb(255, 139, 20)': 1, 'rgb(83, 109, 121)': 1, 'rgb(167, 181, 204)': 1};
    var ACCENT_BG = {'rgb(255, 139, 20)': 1, 'rgb(247, 105, 18)': 1, 'rgb(248, 139, 24)': 1, 'rgb(83, 109, 121)': 1};
    var ACCENT_FILL = {'rgb(255, 139, 20)': 1, 'rgb(248, 139, 24)': 1, 'rgb(83, 109, 121)': 1};

    var SKIP = {IMG: 1, VIDEO: 1, CANVAS: 1, IFRAME: 1, INS: 1, SVG: 1, PATH: 1, SCRIPT: 1, STYLE: 1, HEAD: 1, META: 1, LINK: 1};

    var style = document.createElement('style');
    style.id = 'apkm-dark-fix';
    style.textContent = '.accent_bg.site-header-contents{background-color:' + HEADER_BG + '!important}'
        + '#mobile-slide-header.accent_bg{background-color:' + HEADER_BG + '!important}'
        + '.searchbox-parent.accent_bg{background-color:' + HEADER_BG + '!important}'   // opaque: it covers the title when the search field opens
        + '.infoLink,.infoLink:focus,.infoLink:active,.infoLink:focus-visible,.info-icon{outline:none!important;box-shadow:none!important;border:0!important;-webkit-tap-highlight-color:transparent!important}'   // no frame when the (i) is touched
        + '.info-icon{fill:' + INFO + '!important;color:' + INFO + '!important}'
        + '.info-icon.accent_color{fill:' + ACCENT + '!important;color:' + ACCENT + '!important}'
        + 'html,body{background-color:' + PAGE_BG + '!important}';
    (document.head || document.documentElement).appendChild(style);

    function lightness(color) {
        var m = color.match(/rgba?\(([^)]+)\)/);
        if (!m) return null;
        var p = m[1].split(',').map(function (x) { return parseFloat(x); });
        if (p.length > 3 && p[3] < 0.9) return null;   // transparent: nothing to fix
        return (p[0] + p[1] + p[2]) / 3;
    }

    function fixOne(e) {
        if (!e || e.nodeType !== 1) return;
        var tag = e.tagName.toUpperCase();
        if (e === document.body || e === document.documentElement) return;
        if (tag === 'SCRIPT' || tag === 'STYLE' || tag === 'HEAD' || tag === 'META' || tag === 'LINK') return;
        var cs = getComputedStyle(e);

        // accent: text, icons (svg) and backgrounds that use the orange / blue-grey of the site
        if (ACCENT_TEXT[cs.color]) e.style.setProperty('color', ACCENT, 'important');
        if (ACCENT_FILL[cs.fill]) e.style.setProperty('fill', ACCENT, 'important');
        var bg = cs.backgroundColor;
        if (ACCENT_BG[bg]) {
            e.style.setProperty('background-color', ACCENT, 'important');
            return;
        }

        if (SKIP[tag]) return;
        var l = lightness(bg);
        if (l === null || l < 190) return;
        var target = l >= 250 ? CARD_BG : (l >= 225 ? CARD_BG_2 : CARD_BG_3);
        e.style.setProperty('background-color', target, 'important');
    }

    // The (i) icon is one path: a circle with the "i" cut out. Split it: circle (coloured by CSS) + "i" always white
    function fixInfoSymbol() {
        var sym = document.getElementById('apkm-icon-info');
        if (!sym || sym.getAttribute('data-apkm')) return;
        sym.setAttribute('data-apkm', '1');
        sym.innerHTML = '<path d="M11.675-0.050c-5.775 0-11.7 5.775-11.7 12.325 0 7.075 5.925 11.7 11.7 11.7 7.125 0 12.3-4.625 12.3-11.7 0-6.55-5.175-12.325-12.3-12.325v0z"></path>'
            + '<path fill="' + INFO_GLYPH + '" d="M13.025 18.025h-2.65v-7.1h2.65v7.1zM13.025 8.325h-2.65v-2.6h2.65v2.6z"></path>';
    }

    function fixTree(root) {
        fixInfoSymbol();
        fixOne(root);
        if (!root.querySelectorAll) return;
        var all = root.querySelectorAll('*');
        for (var i = 0; i < all.length; i++) fixOne(all[i]);
    }

    fixTree(document.body || document.documentElement);

    // content added later (lists that load more, popups): fix it too, at most every 300 ms
    var pending = [];
    var timer = null;
    new MutationObserver(function (records) {
        for (var i = 0; i < records.length; i++) {
            for (var j = 0; j < records[i].addedNodes.length; j++) pending.push(records[i].addedNodes[j]);
        }
        if (timer) return;
        timer = setTimeout(function () {
            var list = pending;
            pending = [];
            timer = null;
            for (var k = 0; k < list.length; k++) fixTree(list[k]);
        }, 300);
    }).observe(document.documentElement, {childList: true, subtree: true});
})();
