// Dark theme for apkmirror.com (runs in the WebView only when the app is dark).
// Colours: the app fills the %NAMES% from its colour resources (page_bg, card_bg, card_bg_2, card_bg_3, header_bg in res/values/colors.xml).
(function () {
    if (window.__apkmDark) return;
    window.__apkmDark = true;

    var PAGE_BG = '%PAGE_BG%';    // page background (behind the cards)
    var CARD_BG = '%CARD_BG%';    // cards, lists, panels (lighter than the page)
    var CARD_BG_2 = '%CARD_BG_2%';  // slightly grey light areas (stripes, hover rows, inputs)
    var CARD_BG_3 = '%CARD_BG_3%';  // other light greys
    var HEADER_BG = '%HEADER_BG%';  // site header (search bar and breadcrumbs)

    var SKIP = {IMG: 1, VIDEO: 1, CANVAS: 1, IFRAME: 1, INS: 1, SVG: 1, PATH: 1, SCRIPT: 1, STYLE: 1, HEAD: 1, META: 1, LINK: 1};

    var style = document.createElement('style');
    style.id = 'apkm-dark-fix';
    style.textContent = '.accent_bg.site-header-contents{background-color:' + HEADER_BG + '!important}'
        + '#mobile-slide-header.accent_bg{background-color:' + HEADER_BG + '!important}'
        + '.searchbox-parent.accent_bg{background-color:' + HEADER_BG + '!important}'   // search icon: white on the dark header (it must stay opaque: it covers the title when the search field opens)
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
        if (!e || e.nodeType !== 1 || SKIP[e.tagName]) return;
        if (e === document.body || e === document.documentElement) return;
        var l = lightness(getComputedStyle(e).backgroundColor);
        if (l === null || l < 190) return;
        var target = l >= 250 ? CARD_BG : (l >= 225 ? CARD_BG_2 : CARD_BG_3);
        e.style.setProperty('background-color', target, 'important');
    }

    function fixTree(root) {
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
