// Translates the texts of the site that the app has strings for (side menu).
// The map {"English text": "text in the app language"} is given by the app as the argument.
(function (map) {
    function walk(root) {
        var w = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, null);
        var n;
        while ((n = w.nextNode())) {
            var t = n.nodeValue.trim();
            if (t && map.hasOwnProperty(t) && map[t] !== t) {
                n.nodeValue = n.nodeValue.replace(t, map[t]);
            }
        }
    }
    walk(document.body || document.documentElement);
})
