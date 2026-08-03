/* Theme Switcher — Envios Paraguay CMS */
(function () {
    'use strict';

    function currentTheme() {
        return document.documentElement.getAttribute('data-theme') || 'dark';
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
        document.documentElement.setAttribute('data-theme', next);
        try {
            localStorage.setItem('theme', next);
        } catch (e) {
            /* almacenamiento no disponible */
        }
        syncIcon(next);
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

    document.addEventListener('DOMContentLoaded', function () {
        syncIcon(currentTheme());
        bindButtons();
    });
})();
