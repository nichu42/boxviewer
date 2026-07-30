// Theme toggle: starts in auto (follows OS), click toggles to the opposite
// of whatever is currently rendering (dark ↔ light). Auto is never cycled back to.
// Runs immediately (before paint) to avoid flash of wrong theme.
(function () {
    var stored = localStorage.getItem('theme') || 'auto';
    document.documentElement.dataset.theme = stored;
})();

document.addEventListener('DOMContentLoaded', function () {
    var btn = document.getElementById('theme-toggle');
    if (!btn) return;

    var icons = {
        auto: '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">'
            + '<path d="M12 3 A9 9 0 0 0 12 21 Z" fill="currentColor" stroke="none"/>'
            + '<path d="M12 3 A9 9 0 0 1 12 21" fill="none"/>'
            + '<path d="M12 8 A4 4 0 0 1 12 16" fill="none" stroke-width="1.5"/>'
            + '<line x1="12" y1="2" x2="12" y2="4.5"/>'
            + '<line x1="12" y1="19.5" x2="12" y2="22"/>'
            + '<line x1="18.36" y1="5.64" x2="16.7" y2="7.3" stroke-width="1.5"/>'
            + '<line x1="21" y1="12" x2="18.5" y2="12" stroke-width="1.5"/>'
            + '<line x1="18.36" y1="18.36" x2="16.7" y2="16.7" stroke-width="1.5"/>'
            + '</svg>',
        light: '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">'
            + '<circle cx="12" cy="12" r="4"/>'
            + '<line x1="12" y1="2" x2="12" y2="4.5"/>'
            + '<line x1="12" y1="19.5" x2="12" y2="22"/>'
            + '<line x1="4.22" y1="4.22" x2="5.93" y2="5.93"/>'
            + '<line x1="18.07" y1="18.07" x2="19.78" y2="19.78"/>'
            + '<line x1="2" y1="12" x2="4.5" y2="12"/>'
            + '<line x1="19.5" y1="12" x2="22" y2="12"/>'
            + '<line x1="4.22" y1="19.78" x2="5.93" y2="18.07"/>'
            + '<line x1="18.07" y1="5.93" x2="19.78" y2="4.22"/>'
            + '</svg>',
        dark: '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">'
            + '<path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>'
            + '</svg>'
    };

    // Returns 'dark' or 'light' based on what is actually rendered right now
    function effectiveTheme() {
        var stored = document.documentElement.dataset.theme;
        if (stored === 'dark') return 'dark';
        if (stored === 'light') return 'light';
        return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }

    function apply(theme) {
        document.documentElement.dataset.theme = theme;
        localStorage.setItem('theme', theme);
        btn.innerHTML = icons[theme];
        btn.setAttribute('aria-label', theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode');
    }

    // Initialise button icon: show the stored/auto state
    var initial = localStorage.getItem('theme') || 'auto';
    btn.innerHTML = icons[initial];
    btn.setAttribute('aria-label', effectiveTheme() === 'dark' ? 'Switch to light mode' : 'Switch to dark mode');

    btn.addEventListener('click', function () {
        apply(effectiveTheme() === 'dark' ? 'light' : 'dark');
    });
});
