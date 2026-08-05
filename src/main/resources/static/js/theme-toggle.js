/* Theme Switcher — Envios Paraguay CMS
 * Mecanismo dual: atributo data-theme en <html> + clases light-mode/dark-mode en <html> y <body>.
 * La preferencia se persiste en localStorage bajo la clave 'theme'.
 */
(function () {
    'use strict';

    var STORAGE_KEY = 'theme';

    function currentTheme() {
        var attr = document.documentElement.getAttribute('data-theme');
        if (attr === 'light' || attr === 'dark') {
            return attr;
        }
        return document.documentElement.classList.contains('light-mode') ? 'light' : 'dark';
    }

    function applyTheme(theme) {
        var root = document.documentElement;
        root.setAttribute('data-theme', theme);

        /* Clases duplicadas en <html> y <body> para soportar selectores .light-mode / .dark-mode */
        root.classList.toggle('light-mode', theme === 'light');
        root.classList.toggle('dark-mode', theme === 'dark');
        if (document.body) {
            document.body.classList.toggle('light-mode', theme === 'light');
            document.body.classList.toggle('dark-mode', theme === 'dark');
        }

        syncIcon(theme);
    }

    function syncIcon(theme) {
        var iconName = theme === 'dark' ? 'sun' : 'moon';
        var label = theme === 'dark' ? 'Activar modo claro' : 'Activar modo oscuro';
        document.querySelectorAll('.btn-theme-toggle').forEach(function (btn) {
            btn.setAttribute('aria-label', label);
            btn.setAttribute('title', label);
            var icon = btn.querySelector('[data-lucide]');
            if (icon) {
                icon.setAttribute('data-lucide', iconName);
            }
        });
        if (window.lucide) {
            lucide.createIcons();
        }
    }

    function toggleTheme() {
        var next = currentTheme() === 'dark' ? 'light' : 'dark';
        applyTheme(next);
        try {
            localStorage.setItem(STORAGE_KEY, next);
        } catch (e) {
            /* almacenamiento no disponible */
        }
    }

    function bindButtons() {
        document.querySelectorAll('.btn-theme-toggle').forEach(function (btn) {
            btn.addEventListener('click', function () {
                btn.classList.add('theme-rotating');
                toggleTheme();
                setTimeout(function () {
                    btn.classList.remove('theme-rotating');
                }, 450);
            });
        });
    }

    function init() {
        var saved = null;
        try {
            saved = localStorage.getItem(STORAGE_KEY);
        } catch (e) {
            saved = null;
        }
        applyTheme(saved === 'light' || saved === 'dark' ? saved : 'dark');
        bindButtons();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
