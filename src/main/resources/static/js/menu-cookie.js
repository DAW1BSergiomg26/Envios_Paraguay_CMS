/* menu-cookie.js — Envios Paraguay CMS
 * Theme initialization (before first paint), hamburger menu and cookie banner.
 * Loaded synchronously in <head> to prevent FOUC.
 */
(function () {
    'use strict';

    /* Theme initialization: runs immediately, before the browser paints. */
    var tema = 'dark';
    try {
        var guardado = localStorage.getItem('theme');
        if (guardado === 'light' || guardado === 'dark') {
            tema = guardado;
        }
    } catch (e) {
        /* storage unavailable */
    }
    document.documentElement.setAttribute('data-theme', tema);
    document.documentElement.classList.toggle('light-mode', tema === 'light');
    document.documentElement.classList.toggle('dark-mode', tema === 'dark');

    /* Hamburger menu and cookie banner: depend on DOM. */
    function initHeader() {
        var burger = document.getElementById('btn-hamburguesa');
        var nav = document.getElementById('nav-principal');
        if (burger && nav) {
            burger.addEventListener('click', function () {
                var abierto = nav.classList.toggle('abierto');
                burger.setAttribute('aria-expanded', abierto ? 'true' : 'false');
            });
            document.addEventListener('click', function (e) {
                if (!burger.contains(e.target) && !nav.contains(e.target)) {
                    nav.classList.remove('abierto');
                    burger.setAttribute('aria-expanded', 'false');
                }
            });
        }

        var banner = document.querySelector('.cookie-banner');
        if (banner) {
            var aceptado = false;
            try {
                aceptado = localStorage.getItem('cookies_accepted') === 'true';
            } catch (e) {
                aceptado = false;
            }
            if (aceptado) {
                banner.style.display = 'none';
            }
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initHeader);
    } else {
        initHeader();
    }
})();
