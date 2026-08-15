/* footer.js — Envios Paraguay CMS
 * Inicialización de los iconos de Lucide en el pie de página.
 */
(function () {
    'use strict';

    function createIcons() {
        if (window.lucide) {
            lucide.createIcons();
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', createIcons);
    } else {
        createIcons();
    }
})();
