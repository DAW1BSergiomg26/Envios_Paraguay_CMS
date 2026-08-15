/* tracking-features.js — Envios Paraguay CMS
 * Escáner QR del seguimiento y despliegue de los eventos del historial.
 * Sin manejadores inline: todo se vincula mediante addEventListener.
 */
(function () {
    'use strict';

    /* Escáner de código QR (página de búsqueda de seguimiento). */
    function initQrScanner() {
        var btnAbrir = document.getElementById('btn-abrir-scanner');
        var modal = document.getElementById('qr-modal');
        if (!btnAbrir || !modal) {
            return;
        }

        var btnCerrar = document.getElementById('btn-cerrar-scanner');
        var error = document.getElementById('qr-error');
        var qrScanner = null;

        function abrirEscanner() {
            modal.classList.remove('hidden');
            if (error) {
                error.classList.add('hidden');
            }
            if (typeof Html5Qrcode === 'undefined') {
                if (error) {
                    error.classList.remove('hidden');
                }
                return;
            }
            qrScanner = new Html5Qrcode('qr-reader');
            qrScanner.start({ facingMode: 'environment' }, { fps: 10, qrbox: 220 },
                function (texto) {
                    var m = texto.match(/\/tracking\/([A-Za-z0-9-]+)/);
                    var codigo = m ? m[1] : texto.trim();
                    window.location.href = '/tracking/' + encodeURIComponent(codigo);
                },
                function () { /* sin error */ });
        }

        function cerrarEscanner() {
            modal.classList.add('hidden');
            if (qrScanner) {
                qrScanner.stop().then(function () { qrScanner.clear(); });
                qrScanner = null;
            }
        }

        btnAbrir.addEventListener('click', abrirEscanner);
        if (btnCerrar) {
            btnCerrar.addEventListener('click', cerrarEscanner);
        }
    }

    /* Despliegue de los eventos del historial (página de resultado de seguimiento). */
    function initEventoToggle() {
        document.querySelectorAll('.tracking-event-toggle').forEach(function (boton) {
            boton.addEventListener('click', function () {
                var detalle = boton.nextElementSibling;
                if (detalle) {
                    detalle.classList.toggle('hidden');
                }
            });
        });
    }

    function init() {
        initQrScanner();
        initEventoToggle();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
