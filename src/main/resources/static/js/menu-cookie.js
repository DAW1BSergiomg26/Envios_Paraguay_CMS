/* menu-cookie.js — Envios Paraguay CMS
 * Inicialización del tema (antes del primer pintado), menú hamburguesa y banner de cookies.
 * Cargado en <head> de forma síncrona para evitar el flash de tema (FOUC).
 */
(function () {
    'use strict';

    /* Inicialización del tema: se ejecuta de inmediato, antes de que el navegador pinte. */
    var tema = 'dark';
    try {
        var guardado = localStorage.getItem('theme');
        if (guardado === 'light' || guardado === 'dark') {
            tema = guardado;
        }
    } catch (e) {
        /* almacenamiento no disponible */
    }
    document.documentElement.setAttribute('data-theme', tema);

    /* Menú hamburguesa y banner de cookies: dependen del DOM. */
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
